use crate::backend::{
    parse_remote_timestamp, read_range_response, ByteRange, Entry, RangeResponse, StorageBackend,
    StorageBackendResult, StreamFile,
};
use crate::StorageBackendError;

use futures_util::future::BoxFuture;
use reqwest::header::HeaderValue;
use reqwest::{StatusCode, Url};
use tidetunes_runtime::tokio_runtime;

use std::cmp::Ordering;

use std::sync::{OnceLock, RwLock};
use std::time::Duration;

pub struct Webdav {
    addr: String,
    username: String,
    password: String,
    _is_anonymous: bool,
    last_www_authenticate: RwLock<Option<String>>,
    connect_timeout: Duration,
    client: OnceLock<reqwest::Client>,
}

pub struct BuildWebdavArg {
    pub addr: String,
    pub username: String,
    pub password: String,
    pub is_anonymous: bool,
    pub connect_timeout: Duration,
}

mod webdav_list_types {
    use serde::Deserialize;

    #[derive(Deserialize, Debug)]
    pub struct Collection {}

    #[derive(Deserialize, Debug)]
    pub struct ResourceType {
        pub collection: Option<Collection>,
    }

    #[derive(Deserialize, Debug)]
    pub struct Prop {
        pub displayname: Option<String>,
        pub resourcetype: Option<ResourceType>,
        pub getcontentlength: Option<usize>,
        pub getcontenttype: Option<String>,
        pub getetag: Option<String>,
        pub creationdate: Option<String>,
        pub getlastmodified: Option<String>,
    }

    #[derive(Deserialize, Debug)]
    pub struct PropStat {
        pub prop: Prop,
        pub status: Option<String>,
    }

    #[derive(Deserialize, Debug)]
    pub struct Response {
        pub href: String,
        pub propstat: Vec<PropStat>,
    }

    #[derive(Deserialize, Debug)]
    pub struct Root {
        pub response: Vec<Response>,
    }
}

fn normalize_path(p: String) -> String {
    if p.starts_with('/') {
        p
    } else {
        "/".to_string() + p.as_str()
    }
}

fn build_authorization_header_value(
    www_authenticate: &str,
    username: &str,
    password: &str,
    uri: &str,
    method: &str,
) -> Option<String> {
    if www_authenticate.is_empty() {
        return None;
    }
    let mut pw_client = http_auth::PasswordClient::try_from(www_authenticate).ok()?;
    pw_client
        .respond(&http_auth::PasswordParams {
            username,
            password,
            uri,
            method,
            body: Some(&[]),
        })
        .ok()
}

fn is_auth_error<T>(r: &StorageBackendResult<T>) -> bool {
    matches!(
        r,
        Err(StorageBackendError::RequestFail(error))
            if error.status() == Some(StatusCode::UNAUTHORIZED)
    )
}

fn is_retryable_error<T>(result: &StorageBackendResult<T>) -> bool {
    match result {
        Err(StorageBackendError::RequestFail(error)) => {
            error.is_timeout()
                || error.is_connect()
                || error
                    .status()
                    .is_some_and(|status| status.is_server_error())
        }
        _ => false,
    }
}

impl Webdav {
    pub fn new(arg: BuildWebdavArg) -> Self {
        Self {
            addr: arg.addr,
            username: arg.username,
            password: arg.password,
            _is_anonymous: arg.is_anonymous,
            last_www_authenticate: Default::default(),
            connect_timeout: arg.connect_timeout,
            client: OnceLock::new(),
        }
    }

    fn post_handle_response(&self, resp: &reqwest::Response) {
        let headers = resp.headers();
        let www_authenticate = headers.get(reqwest::header::WWW_AUTHENTICATE);
        if let Some(www_authenticate) = www_authenticate {
            let www_authenticate = www_authenticate.to_str();
            if let Ok(www_authenticate) = www_authenticate {
                {
                    let mut writter = self.last_www_authenticate.write().unwrap();
                    *writter = Some(www_authenticate.to_string());
                }
            }
        }
    }

    fn build_base_header_map(
        &self,
        method: reqwest::Method,
        uri: &reqwest::Url,
    ) -> reqwest::header::HeaderMap {
        let mut header_map = reqwest::header::HeaderMap::new();
        header_map.append(
            reqwest::header::CONTENT_TYPE,
            HeaderValue::from_static("application/xml"),
        );
        header_map.append(
            reqwest::header::ACCEPT,
            HeaderValue::from_static("application/xml"),
        );
        {
            let www_authenticate = self.last_www_authenticate.read().unwrap().clone();
            if let Some(www_authenticate) = www_authenticate {
                let auth = build_authorization_header_value(
                    &www_authenticate,
                    &self.username,
                    &self.password,
                    uri.as_str(),
                    method.as_str(),
                );
                if let Some(auth) = auth {
                    if let Ok(mut value) = HeaderValue::from_str(&auth) {
                        value.set_sensitive(true);
                        header_map.append(reqwest::header::AUTHORIZATION, value);
                    }
                }
            }
        }
        header_map
    }

    fn get_url<const IS_DIR: bool>(&self, p: &str) -> StorageBackendResult<Url> {
        let mut url = reqwest::Url::parse(&self.addr)
            .map_err(|e| StorageBackendError::UrlParseError(e.to_string()))?;
        let base = url.path();
        let mut p = base.trim_end_matches('/').to_string() + "/" + p.trim_start_matches('/');
        if IS_DIR && !p.ends_with('/') {
            p += "/";
        }
        url.set_path(&p);
        Ok(url)
    }

    fn get_href(&self, dir: &str) -> StorageBackendResult<String> {
        let url = reqwest::Url::parse(&self.addr)
            .map_err(|e| StorageBackendError::UrlParseError(e.to_string()))?;
        let base = normalize_path(url.path().to_string());
        Ok(normalize_path(dir.trim_start_matches(base.as_str()).into()))
    }

    async fn list_core(&self, dir: &str) -> StorageBackendResult<reqwest::Response> {
        let url = self.get_url::<true>(dir)?;

        let method = reqwest::Method::from_bytes(b"PROPFIND")
            .map_err(|error| StorageBackendError::UrlParseError(error.to_string()))?;
        let resp = {
            let client = self.build_client()?;
            let headers = self.build_base_header_map(method.clone(), &url);

            tokio_runtime()
                .spawn(async move {
                    client
                        .request(method.clone(), url.clone())
                        .headers(headers)
                        .header("Depth", 1)
                        .body(
                            r#"<?xml version="1.0" ?>
                <D:propfind xmlns:D="DAV:">
                <D:allprop/>
                </D:propfind>"#,
                        )
                        .send()
                        .await
                })
                .await??
        };
        self.post_handle_response(&resp);

        Ok(resp)
    }

    async fn list_impl(&self, dir: &str) -> StorageBackendResult<Vec<Entry>> {
        let resp = self.list_core(dir).await?.error_for_status()?;
        let text: String = resp.text().await?;
        let obj: webdav_list_types::Root = match quick_xml::de::from_str(&text) {
            Ok(obj) => obj,
            Err(error) => {
                tracing::error!("webdav list resp: {text}");
                return Err(error.into());
            }
        };

        let mut ret: Vec<Entry> = Default::default();
        for item in obj.response {
            let path = item.href;
            let Some(prop) = item
                .propstat
                .into_iter()
                .find(|propstat| {
                    propstat
                        .status
                        .as_deref()
                        .is_none_or(|status| status.contains(" 200 "))
                })
                .map(|propstat| propstat.prop)
            else {
                continue;
            };
            let mut name = prop.displayname.unwrap_or_default();
            let is_dir = prop
                .resourcetype
                .and_then(|resource_type| resource_type.collection)
                .is_some();
            let size = prop.getcontentlength;
            let mut path = self.get_href(path.as_str())?;

            if path == "/" {
                continue;
            }
            if path.ends_with("/") {
                path.pop();
            }
            if path == dir || (dir.ends_with('/') && dir[0..dir.len() - 1] == path) {
                continue;
            }
            if name.is_empty() {
                let splited: Vec<&str> = path.split("/").collect();
                if !splited.is_empty() {
                    name = splited.last().copied().unwrap_or_default().to_string();
                }
            }
            name = urlencoding::decode(name.as_str())
                .map(|v| v.to_string())
                .unwrap_or(name);

            ret.push(Entry {
                name,
                path,
                size,
                is_dir,
                remote_id: None,
                parent_remote_id: None,
                mime_type: non_empty(prop.getcontenttype),
                etag: non_empty(prop.getetag),
                ctag: None,
                created_at: non_empty(prop.creationdate)
                    .as_deref()
                    .and_then(parse_remote_timestamp),
                modified_at: non_empty(prop.getlastmodified)
                    .as_deref()
                    .and_then(parse_remote_timestamp),
            });
        }

        ret.sort_by(|lhs, rhs| {
            if lhs.is_dir ^ rhs.is_dir {
                if lhs.is_dir {
                    return Ordering::Less;
                } else {
                    return Ordering::Greater;
                }
            }
            if lhs.path < rhs.path {
                Ordering::Less
            } else {
                Ordering::Greater
            }
        });

        Ok(ret)
    }

    async fn list_with_retry_impl(&self, dir: String) -> StorageBackendResult<Vec<Entry>> {
        let r = self.list_impl(dir.as_str()).await;
        if !is_auth_error(&r) {
            return r;
        }
        self.list_impl(dir.as_str()).await
    }

    async fn get_impl(&self, p: &str, byte_offset: u64) -> StorageBackendResult<StreamFile> {
        let url = self.get_url::<false>(p)?;

        let mut headers = self.build_base_header_map(reqwest::Method::GET, &url);
        headers.insert(
            reqwest::header::RANGE,
            HeaderValue::from_str(format!("bytes={byte_offset}-").as_str()).unwrap(),
        );

        let resp = {
            let client = self.build_client()?;
            tokio_runtime()
                .spawn(async move { client.get(url.clone()).headers(headers).send().await })
                .await??
        };
        let byte_offset = if resp.headers().get(reqwest::header::CONTENT_RANGE).is_some() {
            0
        } else {
            byte_offset
        };
        self.post_handle_response(&resp);

        let res = resp
            .error_for_status()
            .map(|resp| StreamFile::new(resp, byte_offset))?;
        Ok(res)
    }

    async fn get_with_retry_impl(
        &self,
        p: String,
        byte_offset: u64,
    ) -> StorageBackendResult<StreamFile> {
        let r = self.get_impl(p.as_str(), byte_offset).await;
        if !is_auth_error(&r) {
            return r;
        }
        self.get_impl(p.as_str(), byte_offset).await
    }

    async fn get_range_response_impl(
        &self,
        p: &str,
        range: ByteRange,
    ) -> StorageBackendResult<RangeResponse> {
        let url = self.get_url::<false>(p)?;
        let mut headers = self.build_base_header_map(reqwest::Method::GET, &url);
        headers.insert(
            reqwest::header::RANGE,
            HeaderValue::from_str(
                format!("bytes={}-{}", range.start, range.end_inclusive).as_str(),
            )
            .unwrap(),
        );
        let response = {
            let client = self.build_client()?;
            let request_timeout = self.connect_timeout.max(Duration::from_secs(10));
            tokio_runtime()
                .spawn(async move {
                    client
                        .get(url)
                        .headers(headers)
                        .timeout(request_timeout)
                        .send()
                        .await
                })
                .await??
        };
        self.post_handle_response(&response);
        read_range_response(response, range).await
    }

    async fn get_range_response_with_retry_impl(
        &self,
        p: String,
        range: ByteRange,
    ) -> StorageBackendResult<RangeResponse> {
        let result = self.get_range_response_impl(&p, range).await;
        if !is_auth_error(&result) && !is_retryable_error(&result) {
            return result;
        }
        tracing::warn!(path = p, "retrying WebDAV range request");
        self.get_range_response_impl(&p, range).await
    }

    fn build_client(&self) -> StorageBackendResult<reqwest::Client> {
        if let Some(client) = self.client.get() {
            return Ok(client.clone());
        }
        let client = reqwest::Client::builder()
            .connect_timeout(self.connect_timeout)
            .no_proxy()
            .build()?;
        let _ = self.client.set(client.clone());
        Ok(self.client.get().cloned().unwrap_or(client))
    }
}

fn non_empty(value: Option<String>) -> Option<String> {
    value.filter(|value| !value.is_empty())
}

impl StorageBackend for Webdav {
    fn list(&self, dir: String) -> BoxFuture<'_, StorageBackendResult<Vec<Entry>>> {
        Box::pin(self.list_with_retry_impl(dir))
    }

    fn get(&self, p: String, byte_offset: u64) -> BoxFuture<'_, StorageBackendResult<StreamFile>> {
        Box::pin(self.get_with_retry_impl(p, byte_offset))
    }
    fn get_range_response(
        &self,
        p: String,
        range: ByteRange,
    ) -> BoxFuture<'_, StorageBackendResult<RangeResponse>> {
        Box::pin(self.get_range_response_with_retry_impl(p, range))
    }
}

#[cfg(test)]
mod test {
    use std::{convert::Infallible, net::SocketAddr, time::Duration};

    use dav_server::{fakels::FakeLs, localfs::LocalFs, DavHandler};
    use tokio::task::JoinHandle;

    use crate::backend::{ByteRange, StorageBackend};

    use super::{BuildWebdavArg, Webdav};

    struct SetupServerRes {
        addr: String,
        handle: JoinHandle<()>,
    }
    impl SetupServerRes {
        pub fn addr(&self) -> String {
            self.addr.clone()
        }
    }
    impl Drop for SetupServerRes {
        fn drop(&mut self) {
            self.handle.abort();
        }
    }

    async fn setup_server(p: &str) -> SetupServerRes {
        let dav_server = DavHandler::builder()
            .filesystem(LocalFs::new(p, false, false, false))
            .locksystem(FakeLs::new())
            .autoindex(true)
            .build_handler();

        let addr: SocketAddr = ([127, 0, 0, 1], 0).into();
        let make_service = hyper::service::make_service_fn(move |_| {
            let dav_server = dav_server.clone();
            async move {
                let func = move |req| {
                    let dav_server = dav_server.clone();
                    async move { Ok::<_, Infallible>(dav_server.handle(req).await) }
                };
                Ok::<_, Infallible>(hyper::service::service_fn(func))
            }
        });

        let server = hyper::Server::bind(&addr).serve(make_service);
        let port = server.local_addr().port();

        let handle = tokio::spawn(async move {
            server.await.unwrap();
        });
        tokio::time::sleep(Duration::from_millis(200)).await;

        SetupServerRes {
            addr: format!("http://127.0.0.1:{port}"),
            handle,
        }
    }

    #[tokio::test]
    async fn test_list() {
        let server = setup_server("test/assets/case_list").await;

        let backend = Webdav::new(BuildWebdavArg {
            addr: server.addr(),
            username: Default::default(),
            password: Default::default(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(10),
        });
        let list = backend.list("/".to_string()).await.unwrap();
        assert_eq!(list.len(), 2);
        assert_eq!(list[0].path, "/a.txt");
        assert_eq!(list[1].path, "/b.log.txt");
    }

    #[tokio::test]
    async fn test_file_content_1() {
        let server = setup_server("test/assets/case_content").await;

        let backend = Webdav::new(BuildWebdavArg {
            addr: server.addr(),
            username: Default::default(),
            password: Default::default(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(10),
        });
        let mut list = backend.list("/".to_string()).await.unwrap();
        assert_eq!(list.len(), 1);

        let item = list.pop().unwrap();
        assert_eq!(item.path, "/a.bin");
        assert_eq!(item.size, Some(3));

        let file = backend.get(item.path, 0).await.unwrap();
        assert_eq!(file.size(), Some(3));

        let stream = file.into_rx();
        let chunk = stream.recv().await;
        assert!(chunk.is_ok());
        let chunk = chunk.unwrap().unwrap();
        assert_eq!(chunk.as_ref(), [49, 50, 51]);
    }

    #[tokio::test]
    async fn test_file_content_2() {
        let server = setup_server("test/assets/case_content_2").await;

        let backend = Webdav::new(BuildWebdavArg {
            addr: server.addr(),
            username: Default::default(),
            password: Default::default(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(10),
        });
        let list = backend.list("/".to_string()).await.unwrap();
        assert_eq!(list.len(), 2);
        let item = &list[0];
        assert_eq!(item.path, "/b-folder");
        assert_eq!(item.size, None);
        let item = &list[1];
        assert_eq!(item.path, "/a.bin");
        assert_eq!(item.size, Some(3));

        let list = backend.list("/b-folder".to_string()).await.unwrap();
        assert_eq!(list.len(), 1);
        let item = &list[0];
        assert_eq!(item.path, "/b-folder/b.bin");
        assert_eq!(item.size, Some(3));

        let file = backend.get(item.path.to_string(), 0).await.unwrap();
        assert_eq!(file.size(), Some(3));

        let stream = file.into_rx();
        let chunk = stream.recv().await;
        assert!(chunk.is_ok());
        let chunk = chunk.unwrap().unwrap();
        assert_eq!(chunk.as_ref(), [49, 50, 51]);
    }

    #[tokio::test]
    async fn test_file_content_1_partial_stream() {
        let server = setup_server("test/assets/case_content").await;

        let backend = Webdav::new(BuildWebdavArg {
            addr: server.addr(),
            username: Default::default(),
            password: Default::default(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(10),
        });
        let file = backend.get("/a.bin".to_string(), 2).await.unwrap();
        assert_eq!(file.size(), Some(1));

        let stream = file.into_rx();
        let chunk = stream.recv().await;
        assert!(chunk.is_ok());
        let chunk = chunk.unwrap().unwrap();
        assert_eq!(chunk.as_ref(), [51]);
    }

    #[tokio::test]
    async fn test_file_content_1_bounded_range() {
        let server = setup_server("test/assets/case_content").await;
        let backend = Webdav::new(BuildWebdavArg {
            addr: server.addr(),
            username: String::new(),
            password: String::new(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(5),
        });

        let bytes = backend
            .get_range("/a.bin".to_string(), ByteRange::new(1, 2).unwrap())
            .await
            .unwrap();

        assert_eq!(bytes.as_ref(), b"23");
    }

    #[tokio::test]
    async fn test_file_content_1_partial_bytes() {
        let server = setup_server("test/assets/case_content").await;

        let backend = Webdav::new(BuildWebdavArg {
            addr: server.addr(),
            username: Default::default(),
            password: Default::default(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(10),
        });
        let file = backend.get("/a.bin".to_string(), 2).await.unwrap();
        assert_eq!(file.size(), Some(1));

        let chunk = file.bytes().await.unwrap();
        assert_eq!(chunk.as_ref(), [51]);
    }
}
