use crate::backend::{
    parse_remote_timestamp, read_range_response, ByteRange, Entry, RangeResponse, StorageBackend,
    StorageBackendResult, StreamFile, WebDavSyncItem, WebDavSyncPage,
};
use crate::StorageBackendError;

use futures_util::future::BoxFuture;
use reqwest::header::HeaderValue;
use reqwest::{StatusCode, Url};
use tidetunes_async_runtime::tokio_runtime;

use std::cmp::Ordering;

use std::sync::{Mutex, OnceLock, RwLock};
use std::time::{Duration, Instant};

const PROPFIND_BODY: &str = r#"<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    <D:displayname/>
    <D:resourcetype/>
    <D:getcontentlength/>
    <D:getcontenttype/>
    <D:getetag/>
    <D:creationdate/>
    <D:getlastmodified/>
  </D:prop>
</D:propfind>"#;
const MAX_REQUEST_RETRIES: usize = 3;
const RETRY_DELAYS: [Duration; MAX_REQUEST_RETRIES] = [
    Duration::from_millis(250),
    Duration::from_millis(500),
    Duration::from_secs(1),
];

struct AbortTaskOnDrop(Option<tokio::task::AbortHandle>);

impl Drop for AbortTaskOnDrop {
    fn drop(&mut self) {
        if let Some(handle) = self.0.take() {
            handle.abort();
        }
    }
}

pub struct Webdav {
    addr: String,
    username: String,
    password: String,
    _is_anonymous: bool,
    last_www_authenticate: RwLock<Option<String>>,
    connect_timeout: Duration,
    client: OnceLock<reqwest::Client>,
    request_cooldown: Mutex<Option<Instant>>,
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

    #[derive(Deserialize, Debug, Default)]
    pub struct ResourceType {
        pub collection: Option<Collection>,
    }

    #[derive(Deserialize, Debug, Default)]
    pub struct Prop {
        pub displayname: Option<String>,
        pub resourcetype: Option<ResourceType>,
        pub getcontentlength: Option<String>,
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
        pub status: Option<String>,
        #[serde(default)]
        pub propstat: Vec<PropStat>,
    }

    #[derive(Deserialize, Debug)]
    pub struct Root {
        #[serde(default)]
        pub response: Vec<Response>,
        #[serde(rename = "sync-token")]
        pub sync_token: Option<String>,
    }

    impl Prop {
        pub fn merge_missing(&mut self, other: Self) {
            self.displayname = self.displayname.take().or(other.displayname);
            self.resourcetype = self.resourcetype.take().or(other.resourcetype);
            self.getcontentlength = self.getcontentlength.take().or(other.getcontentlength);
            self.getcontenttype = self.getcontenttype.take().or(other.getcontenttype);
            self.getetag = self.getetag.take().or(other.getetag);
            self.creationdate = self.creationdate.take().or(other.creationdate);
            self.getlastmodified = self.getlastmodified.take().or(other.getlastmodified);
        }
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

fn is_auth_error<T>(result: &StorageBackendResult<T>) -> bool {
    matches!(result, Err(error) if error.is_unauthorized())
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

fn error_is_retryable(error: &StorageBackendError) -> bool {
    match error {
        StorageBackendError::RequestFail(error) => {
            error.is_timeout()
                || error.is_connect()
                || error.status().is_some_and(is_retryable_status)
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
            request_cooldown: Mutex::new(None),
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
        let raw_path = reqwest::Url::parse(dir)
            .ok()
            .map(|href| href.path().to_string())
            .unwrap_or_else(|| dir.split(['?', '#']).next().unwrap_or(dir).to_string());
        let decoded_path = urlencoding::decode(&raw_path)
            .map_err(|error| StorageBackendError::UrlParseError(error.to_string()))?
            .into_owned();
        let decoded_base = urlencoding::decode(url.path())
            .map_err(|error| StorageBackendError::UrlParseError(error.to_string()))?
            .into_owned();
        let base = normalize_path(decoded_base)
            .trim_end_matches('/')
            .to_string();
        let relative = decoded_path
            .strip_prefix(&base)
            .filter(|suffix| suffix.is_empty() || suffix.starts_with('/'))
            .unwrap_or(decoded_path.as_str());
        Ok(normalize_path(relative.to_string()))
    }

    async fn send_xml_once(
        &self,
        method: reqwest::Method,
        url: reqwest::Url,
        depth: &'static str,
        body: String,
    ) -> StorageBackendResult<reqwest::Response> {
        self.wait_for_request_cooldown().await;
        let client = self.build_client()?;
        let headers = self.build_base_header_map(method.clone(), &url);
        let task = tokio_runtime().spawn(async move {
            client
                .request(method.clone(), url.clone())
                .headers(headers)
                .header("Depth", depth)
                .body(body)
                .send()
                .await
        });
        let mut abort_on_drop = AbortTaskOnDrop(Some(task.abort_handle()));
        let resp = task.await??;
        abort_on_drop.0 = None;
        self.post_handle_response(&resp);

        Ok(resp)
    }

    async fn request_xml_with_retry(
        &self,
        method: reqwest::Method,
        url: reqwest::Url,
        depth: &'static str,
        body: String,
    ) -> StorageBackendResult<reqwest::Response> {
        let mut retry_count = 0;
        let mut auth_retried = false;
        loop {
            match self
                .send_xml_once(method.clone(), url.clone(), depth, body.clone())
                .await
            {
                Ok(response) => {
                    let status = response.status();
                    if status == StatusCode::UNAUTHORIZED && !auth_retried {
                        auth_retried = true;
                        continue;
                    }
                    if is_retryable_status(status) && retry_count < MAX_REQUEST_RETRIES {
                        let delay = retry_after(&response).unwrap_or(RETRY_DELAYS[retry_count]);
                        retry_count += 1;
                        if matches!(
                            status,
                            StatusCode::TOO_MANY_REQUESTS | StatusCode::SERVICE_UNAVAILABLE
                        ) {
                            self.extend_request_cooldown(delay);
                        }
                        tracing::warn!(
                            method = method.as_str(),
                            status = status.as_u16(),
                            retry_count,
                            "retrying WebDAV request after a transient response"
                        );
                        tokio::time::sleep(delay).await;
                        continue;
                    }
                    if is_retryable_status(status) {
                        return Err(StorageBackendError::RetryExhausted(format!(
                            "HTTP {}",
                            status.as_u16()
                        )));
                    }
                    return Ok(response);
                }
                Err(error) if error_is_retryable(&error) && retry_count < MAX_REQUEST_RETRIES => {
                    let delay = RETRY_DELAYS[retry_count];
                    retry_count += 1;
                    tracing::warn!(
                        method = method.as_str(),
                        retry_count,
                        "retrying WebDAV request after a transport failure"
                    );
                    tokio::time::sleep(delay).await;
                }
                Err(error) if error_is_retryable(&error) => {
                    return Err(StorageBackendError::RetryExhausted(error.to_string()));
                }
                Err(error) => return Err(error),
            }
        }
    }

    async fn wait_for_request_cooldown(&self) {
        let delay = self
            .request_cooldown
            .lock()
            .unwrap()
            .as_ref()
            .and_then(|deadline| deadline.checked_duration_since(Instant::now()));
        if let Some(delay) = delay {
            tokio::time::sleep(delay).await;
        }
    }

    fn extend_request_cooldown(&self, delay: Duration) {
        let requested_deadline = Instant::now() + delay;
        let mut cooldown = self.request_cooldown.lock().unwrap();
        if cooldown.is_none_or(|deadline| deadline < requested_deadline) {
            *cooldown = Some(requested_deadline);
        }
    }

    async fn list_core(&self, dir: &str) -> StorageBackendResult<reqwest::Response> {
        let url = self.get_url::<true>(dir)?;
        let method = reqwest::Method::from_bytes(b"PROPFIND")
            .map_err(|error| StorageBackendError::UrlParseError(error.to_string()))?;
        self.request_xml_with_retry(method, url, "1", PROPFIND_BODY.to_string())
            .await
    }

    fn parse_list_response(&self, dir: &str, text: &str) -> StorageBackendResult<Vec<Entry>> {
        let obj: webdav_list_types::Root = match quick_xml::de::from_str(text) {
            Ok(obj) => obj,
            Err(error) => {
                tracing::error!("could not parse WebDAV PROPFIND response");
                return Err(error.into());
            }
        };

        let mut ret: Vec<Entry> = Default::default();
        for item in obj.response {
            let path = item.href;
            let Some(prop) = merge_successful_props(item.propstat) else {
                continue;
            };
            let mut name = prop.displayname.unwrap_or_default();
            let is_dir = prop
                .resourcetype
                .and_then(|resource_type| resource_type.collection)
                .is_some();
            let size = parse_content_length(prop.getcontentlength.as_deref());
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

    async fn list_impl(&self, dir: &str) -> StorageBackendResult<Vec<Entry>> {
        let resp = self.list_core(dir).await?.error_for_status()?;
        let text = resp.text().await?;
        self.parse_list_response(dir, &text)
    }

    async fn list_with_retry_impl(&self, dir: String) -> StorageBackendResult<Vec<Entry>> {
        self.list_impl(dir.as_str()).await
    }

    async fn webdav_sync_impl(
        &self,
        root_path: &str,
        sync_token: Option<&str>,
    ) -> StorageBackendResult<WebDavSyncPage> {
        let url = self.get_url::<true>(root_path)?;
        let method = reqwest::Method::from_bytes(b"REPORT")
            .map_err(|error| StorageBackendError::UrlParseError(error.to_string()))?;
        let body = sync_collection_body(sync_token);
        let response = self
            .request_xml_with_retry(method, url, "infinity", body)
            .await?;
        let status = response.status();
        if matches!(
            status,
            StatusCode::METHOD_NOT_ALLOWED | StatusCode::NOT_IMPLEMENTED
        ) {
            return Err(StorageBackendError::DeltaNotSupported);
        }
        let status_error = response.error_for_status_ref().err();
        let text = response.text().await?;
        if matches!(status, StatusCode::CONFLICT | StatusCode::GONE)
            || (status == StatusCode::FORBIDDEN && text.contains("valid-sync-token"))
        {
            return Err(StorageBackendError::DeltaResyncRequired);
        }
        if status == StatusCode::BAD_REQUEST && sync_token.is_none() {
            return Err(StorageBackendError::DeltaNotSupported);
        }
        if let Some(error) = status_error {
            return Err(error.into());
        }
        self.parse_sync_response(root_path, &text)
    }

    fn parse_sync_response(
        &self,
        root_path: &str,
        text: &str,
    ) -> StorageBackendResult<WebDavSyncPage> {
        let root: webdav_list_types::Root = quick_xml::de::from_str(text)?;
        let sync_token = root
            .sync_token
            .filter(|token| !token.is_empty())
            .ok_or(StorageBackendError::ParseXMLFail)?;
        let normalized_root = normalize_collection_path(root_path);
        let mut items = Vec::new();
        for response in root.response {
            let path = normalize_collection_path(&self.get_href(&response.href)?);
            if path == normalized_root {
                continue;
            }
            let response_deleted = response.status.as_deref().is_some_and(is_missing_status);
            let missing_propstat = response
                .propstat
                .iter()
                .any(|propstat| propstat.status.as_deref().is_some_and(is_missing_status));
            let prop = merge_successful_props(response.propstat);
            if response_deleted || (prop.is_none() && missing_propstat) {
                items.push(WebDavSyncItem {
                    name: path.rsplit('/').next().map(str::to_string),
                    path,
                    size: None,
                    is_dir: false,
                    deleted: true,
                    mime_type: None,
                    etag: None,
                    created_at: None,
                    modified_at: None,
                });
                continue;
            }
            let Some(prop) = prop else {
                continue;
            };
            let name = non_empty(prop.displayname).or_else(|| {
                path.rsplit('/')
                    .next()
                    .filter(|name| !name.is_empty())
                    .map(str::to_string)
            });
            items.push(WebDavSyncItem {
                path,
                name,
                size: parse_content_length(prop.getcontentlength.as_deref()),
                is_dir: prop
                    .resourcetype
                    .and_then(|resource_type| resource_type.collection)
                    .is_some(),
                deleted: false,
                mime_type: non_empty(prop.getcontenttype),
                etag: non_empty(prop.getetag),
                created_at: non_empty(prop.creationdate)
                    .as_deref()
                    .and_then(parse_remote_timestamp),
                modified_at: non_empty(prop.getlastmodified)
                    .as_deref()
                    .and_then(parse_remote_timestamp),
            });
        }
        Ok(WebDavSyncPage { items, sync_token })
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

fn parse_content_length(value: Option<&str>) -> Option<usize> {
    value.filter(|value| !value.is_empty())?.parse().ok()
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
    fn webdav_sync(
        &self,
        root_path: String,
        sync_token: Option<String>,
    ) -> BoxFuture<'_, StorageBackendResult<WebDavSyncPage>> {
        Box::pin(async move {
            self.webdav_sync_impl(&root_path, sync_token.as_deref())
                .await
        })
    }
}

fn merge_successful_props(
    propstats: Vec<webdav_list_types::PropStat>,
) -> Option<webdav_list_types::Prop> {
    let mut merged: Option<webdav_list_types::Prop> = None;
    for propstat in propstats
        .into_iter()
        .filter(|propstat| propstat.status.as_deref().is_none_or(is_success_status))
    {
        if let Some(current) = &mut merged {
            current.merge_missing(propstat.prop);
        } else {
            merged = Some(propstat.prop);
        }
    }
    merged
}

fn is_success_status(status: &str) -> bool {
    status.split_ascii_whitespace().nth(1) == Some("200")
}

fn is_missing_status(status: &str) -> bool {
    matches!(status.split_ascii_whitespace().nth(1), Some("404" | "410"))
}

fn is_retryable_status(status: StatusCode) -> bool {
    matches!(
        status,
        StatusCode::TOO_MANY_REQUESTS
            | StatusCode::INTERNAL_SERVER_ERROR
            | StatusCode::BAD_GATEWAY
            | StatusCode::SERVICE_UNAVAILABLE
            | StatusCode::GATEWAY_TIMEOUT
    )
}

fn retry_after(response: &reqwest::Response) -> Option<Duration> {
    response
        .headers()
        .get(reqwest::header::RETRY_AFTER)?
        .to_str()
        .ok()?
        .parse::<u64>()
        .ok()
        .map(Duration::from_secs)
}

fn normalize_collection_path(path: &str) -> String {
    let normalized = normalize_path(path.replace('\\', "/"));
    if normalized == "/" {
        normalized
    } else {
        normalized.trim_end_matches('/').to_string()
    }
}

fn sync_collection_body(sync_token: Option<&str>) -> String {
    let token = sync_token.map(xml_escape).unwrap_or_default();
    format!(
        r#"<?xml version="1.0" encoding="utf-8"?>
<D:sync-collection xmlns:D="DAV:">
  <D:sync-token>{token}</D:sync-token>
  <D:sync-level>infinite</D:sync-level>
  <D:prop>
    <D:displayname/>
    <D:resourcetype/>
    <D:getcontentlength/>
    <D:getcontenttype/>
    <D:getetag/>
    <D:creationdate/>
    <D:getlastmodified/>
  </D:prop>
</D:sync-collection>"#
    )
}

fn xml_escape(value: &str) -> String {
    value
        .replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
        .replace('\'', "&apos;")
}

#[cfg(test)]
mod test {
    use std::{
        convert::Infallible,
        net::SocketAddr,
        sync::{
            atomic::{AtomicUsize, Ordering},
            Arc, Mutex,
        },
        time::Duration,
    };

    use dav_server::{fakels::FakeLs, localfs::LocalFs, DavHandler};
    use hyper::{Body, Request, Response};
    use reqwest::StatusCode;
    use tokio::task::JoinHandle;

    use crate::backend::{ByteRange, StorageBackend};
    use crate::StorageBackendError;

    use super::{sync_collection_body, BuildWebdavArg, Webdav, PROPFIND_BODY};

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

    fn backend(addr: String) -> Webdav {
        Webdav::new(BuildWebdavArg {
            addr,
            username: String::new(),
            password: String::new(),
            is_anonymous: true,
            connect_timeout: Duration::from_secs(5),
        })
    }

    async fn setup_recording_server(
        handler: impl Fn(usize, Request<Body>) -> Response<Body> + Send + Sync + 'static,
    ) -> (SetupServerRes, Arc<Mutex<Vec<(String, String)>>>) {
        let requests = Arc::new(Mutex::new(Vec::new()));
        let request_count = Arc::new(AtomicUsize::new(0));
        let handler = Arc::new(handler);
        let addr: SocketAddr = ([127, 0, 0, 1], 0).into();
        let make_service = hyper::service::make_service_fn({
            let requests = Arc::clone(&requests);
            let request_count = Arc::clone(&request_count);
            move |_| {
                let requests = Arc::clone(&requests);
                let request_count = Arc::clone(&request_count);
                let handler = Arc::clone(&handler);
                async move {
                    Ok::<_, Infallible>(hyper::service::service_fn(move |request| {
                        let requests = Arc::clone(&requests);
                        let request_count = Arc::clone(&request_count);
                        let handler = Arc::clone(&handler);
                        async move {
                            let index = request_count.fetch_add(1, Ordering::AcqRel);
                            let (parts, body) = request.into_parts();
                            let body = hyper::body::to_bytes(body).await.unwrap();
                            requests.lock().unwrap().push((
                                parts.method.as_str().to_string(),
                                String::from_utf8_lossy(&body).into_owned(),
                            ));
                            let request = Request::from_parts(parts, Body::from(body));
                            Ok::<_, Infallible>(handler(index, request))
                        }
                    }))
                }
            }
        });
        let server = hyper::Server::bind(&addr).serve(make_service);
        let port = server.local_addr().port();
        let handle = tokio::spawn(async move { server.await.unwrap() });
        (
            SetupServerRes {
                addr: format!("http://127.0.0.1:{port}"),
                handle,
            },
            requests,
        )
    }

    #[tokio::test]
    async fn propfind_requests_only_scan_properties() {
        let (server, requests) = setup_recording_server(|_, _| {
            Response::builder()
                .status(StatusCode::MULTI_STATUS)
                .body(Body::from("<D:multistatus xmlns:D=\"DAV:\"/>"))
                .unwrap()
        })
        .await;

        backend(server.addr()).list("/".to_string()).await.unwrap();

        let requests = requests.lock().unwrap();
        assert_eq!(requests.len(), 1);
        assert_eq!(requests[0].0, "PROPFIND");
        assert_eq!(requests[0].1, PROPFIND_BODY);
        assert!(!requests[0].1.contains("allprop"));
        assert!(requests[0].1.contains("<D:getetag/>"));
        assert!(requests[0].1.contains("<D:getlastmodified/>"));
    }

    #[test]
    fn parses_missing_properties_and_merges_successful_propstats() {
        let backend = backend("https://example.com/dav".to_string());
        let entries = backend
            .parse_list_response(
                "/Music",
                r#"<x:multistatus xmlns:x="DAV:">
                  <x:response>
                    <x:href>/dav/Music/Album%20One/song.flac</x:href>
                    <x:propstat>
                      <x:prop><x:displayname>ignored.flac</x:displayname></x:prop>
                      <x:status>HTTP/1.1 404 Not Found</x:status>
                    </x:propstat>
                    <x:propstat>
                      <x:prop><x:displayname>song.flac</x:displayname><x:getcontentlength>12</x:getcontentlength></x:prop>
                      <x:status>HTTP/1.1 200 OK</x:status>
                    </x:propstat>
                    <x:propstat>
                      <x:prop><x:getcontenttype>audio/flac</x:getcontenttype></x:prop>
                      <x:status>HTTP/1.1 200 OK</x:status>
                    </x:propstat>
                  </x:response>
                </x:multistatus>"#,
            )
            .unwrap();

        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].path, "/Music/Album One/song.flac");
        assert_eq!(entries[0].name, "song.flac");
        assert_eq!(entries[0].size, Some(12));
        assert_eq!(entries[0].mime_type.as_deref(), Some("audio/flac"));
        assert_eq!(entries[0].etag, None);
        assert_eq!(entries[0].modified_at, None);
    }

    #[test]
    fn parses_sync_collection_changes_deletions_and_token() {
        let backend = backend("https://example.com/dav".to_string());
        let page = backend
            .parse_sync_response(
                "/Music",
                r#"<D:multistatus xmlns:D="DAV:">
                  <D:response>
                    <D:href>/dav/Music/new.flac</D:href>
                    <D:propstat><D:prop>
                      <D:displayname>new.flac</D:displayname>
                      <D:getcontentlength>42</D:getcontentlength>
                      <D:getetag>W/&quot;stable&quot;</D:getetag>
                    </D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat>
                  </D:response>
                  <D:response>
                    <D:href>/dav/Music/deleted.flac</D:href>
                    <D:status>HTTP/1.1 404 Not Found</D:status>
                  </D:response>
                  <D:sync-token>https://example.com/token/2</D:sync-token>
                </D:multistatus>"#,
            )
            .unwrap();

        assert_eq!(page.sync_token, "https://example.com/token/2");
        assert_eq!(page.items.len(), 2);
        assert_eq!(page.items[0].path, "/Music/new.flac");
        assert_eq!(page.items[0].etag.as_deref(), Some("W/\"stable\""));
        assert!(!page.items[0].deleted);
        assert_eq!(page.items[1].path, "/Music/deleted.flac");
        assert!(page.items[1].deleted);
    }

    #[tokio::test]
    async fn retries_429_using_retry_after_then_succeeds() {
        let (server, requests) = setup_recording_server(|index, _| {
            if index == 0 {
                Response::builder()
                    .status(StatusCode::TOO_MANY_REQUESTS)
                    .header(reqwest::header::RETRY_AFTER, "0")
                    .body(Body::empty())
                    .unwrap()
            } else {
                Response::builder()
                    .status(StatusCode::MULTI_STATUS)
                    .body(Body::from("<D:multistatus xmlns:D=\"DAV:\"/>"))
                    .unwrap()
            }
        })
        .await;

        backend(server.addr()).list("/".to_string()).await.unwrap();

        assert_eq!(requests.lock().unwrap().len(), 2);
    }

    #[tokio::test]
    async fn invalid_sync_token_requests_full_resync() {
        let (server, requests) = setup_recording_server(|_, _| {
            Response::builder()
                .status(StatusCode::FORBIDDEN)
                .body(Body::from(
                    "<D:error xmlns:D=\"DAV:\"><D:valid-sync-token/></D:error>",
                ))
                .unwrap()
        })
        .await;

        let error = backend(server.addr())
            .webdav_sync("/Music".to_string(), Some("old-token".to_string()))
            .await
            .unwrap_err();

        assert!(matches!(error, StorageBackendError::DeltaResyncRequired));
        let requests = requests.lock().unwrap();
        assert_eq!(requests[0].0, "REPORT");
        assert_eq!(requests[0].1, sync_collection_body(Some("old-token")));
    }

    #[tokio::test]
    async fn unsupported_sync_collection_falls_back_without_retrying() {
        let (server, requests) = setup_recording_server(|_, _| {
            Response::builder()
                .status(StatusCode::METHOD_NOT_ALLOWED)
                .body(Body::empty())
                .unwrap()
        })
        .await;

        let error = backend(server.addr())
            .webdav_sync("/Music".to_string(), None)
            .await
            .unwrap_err();

        assert!(matches!(error, StorageBackendError::DeltaNotSupported));
        let requests = requests.lock().unwrap();
        assert_eq!(requests.len(), 1);
        assert_eq!(requests[0].0, "REPORT");
    }

    #[tokio::test]
    async fn sync_collection_returns_five_changes_in_one_report() {
        let response_xml = format!(
            "<D:multistatus xmlns:D=\"DAV:\">{}<D:sync-token>token-2</D:sync-token></D:multistatus>",
            (0..5)
                .map(|index| format!(
                    "<D:response><D:href>/Music/song-{index}.flac</D:href><D:propstat><D:prop><D:displayname>song-{index}.flac</D:displayname><D:getcontentlength>42</D:getcontentlength><D:getetag>&quot;etag-{index}&quot;</D:getetag></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response>"
                ))
                .collect::<String>()
        );
        let (server, requests) = setup_recording_server(move |_, _| {
            Response::builder()
                .status(StatusCode::MULTI_STATUS)
                .body(Body::from(response_xml.clone()))
                .unwrap()
        })
        .await;

        let page = backend(server.addr())
            .webdav_sync("/Music".to_string(), Some("token-1".to_string()))
            .await
            .unwrap();

        assert_eq!(page.items.len(), 5);
        assert_eq!(page.sync_token, "token-2");
        let requests = requests.lock().unwrap();
        assert_eq!(requests.len(), 1);
        assert_eq!(requests[0].0, "REPORT");
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
