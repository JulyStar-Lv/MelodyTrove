use crate::{HostApiDispatcher, PluginRuntimeError};
use aes::{Aes128, Aes192, Aes256};
use base64::{
    engine::general_purpose::{STANDARD, URL_SAFE_NO_PAD},
    Engine,
};
use cipher::{block_padding::Pkcs7, BlockDecryptMut, BlockEncryptMut, KeyInit};
use flate2::read::ZlibDecoder;
use md5::{Digest, Md5};
use serde_json::{json, Map, Value};
use sha2::Sha256;
use std::{
    fs,
    io::Read,
    net::{IpAddr, ToSocketAddrs},
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};
use url::Url;
mod xml;

pub const SUPPORTED_HOST_APIS: &[&str] = &[
    "app.info",
    "app.userAgent",
    "runtime.info",
    "cache.get",
    "cache.set",
    "cache.remove",
    "cache.clear",
    "crypto.md5",
    "crypto.aesEcbPkcs5EncryptBase64",
    "crypto.aesEcbPkcs5EncryptHex",
    "crypto.aesEcbPkcs5DecryptBase64ToText",
    "base64.encodeText",
    "base64.decodeText",
    "base64.dropBytes",
    "base64.decodeBytes",
    "base64.encodeBytes",
    "base64.encodeUrlText",
    "base64.decodeUrlText",
    "base64.encodeUrlBytes",
    "base64.decodeUrlBytes",
    "base64.toUrl",
    "base64.fromUrl",
    "bytes.xor",
    "bytes.xorBase64",
    "compression.inflateBytesToText",
    "compression.inflateBase64ToText",
    "http.getText",
    "http.postText",
    "http.postBytes",
    "http.get",
    "http.post",
    "http.getBytes",
    "http.postBytesResponse",
    "log.debug",
    "log.warn",
    "log.error",
    "xml.getRootAttributes",
    "xml.findElements",
    "xml.replaceChildrenByAttr",
    "xml.removeElements",
];

#[derive(Clone)]
pub struct HostApiOptions {
    pub plugin_id: String,
    pub plugin_name: String,
    pub app_name: String,
    pub package_name: String,
    pub app_version_name: String,
    pub app_version_code: u64,
    pub build_type: String,
    pub debug: bool,
    pub cache_directory: PathBuf,
    pub max_cache_value_bytes: usize,
    pub max_cache_bytes: usize,
    pub max_inflate_bytes: usize,
    pub allow_http: bool,
    pub allow_https: bool,
    pub allow_private_network: bool,
    pub max_http_request_bytes: usize,
    pub max_http_response_bytes: usize,
}
impl Default for HostApiOptions {
    fn default() -> Self {
        Self {
            plugin_id: "default".into(),
            plugin_name: "Plugin".into(),
            app_name: "TideTunes".into(),
            package_name: "com.github.tidetunes".into(),
            app_version_name: "0.0.0".into(),
            app_version_code: 0,
            build_type: "release".into(),
            debug: false,
            cache_directory: PathBuf::new(),
            max_cache_value_bytes: 256 * 1024,
            max_cache_bytes: 4 * 1024 * 1024,
            max_inflate_bytes: 16 * 1024 * 1024,
            allow_http: false,
            allow_https: true,
            allow_private_network: false,
            max_http_request_bytes: 4 * 1024 * 1024,
            max_http_response_bytes: 16 * 1024 * 1024,
        }
    }
}
pub struct HostApi {
    options: HostApiOptions,
}
impl HostApi {
    pub fn new(options: HostApiOptions) -> Self {
        Self { options }
    }
    fn value(v: Value) -> Result<String, PluginRuntimeError> {
        serde_json::to_string(&json!({"value":v})).map_err(internal)
    }
    fn cache_dir(&self) -> PathBuf {
        self.options
            .cache_directory
            .join("plugins")
            .join(sha256_hex(self.options.plugin_id.as_bytes()))
    }
    fn cache_file(&self, key: &str) -> Result<PathBuf, PluginRuntimeError> {
        if key.trim().is_empty() {
            return Err(host_error("cache key is blank"));
        }
        Ok(self
            .cache_dir()
            .join(format!("{}.json", sha256_hex(key.trim().as_bytes()))))
    }
    fn cache_get(&self, key: &str) -> String {
        let Ok(path) = self.cache_file(key) else {
            return String::new();
        };
        let Ok(data) = fs::read(&path) else {
            return String::new();
        };
        let Ok(v) = serde_json::from_slice::<Value>(&data) else {
            let _ = fs::remove_file(path);
            return String::new();
        };
        let expires = v["expiresAt"].as_u64().unwrap_or(0);
        if expires > 0 && expires <= now_ms() {
            let _ = fs::remove_file(path);
            return String::new();
        }
        v["value"].as_str().unwrap_or("").to_owned()
    }
    fn cache_set(&self, key: &str, value: &str, ttl: u64) -> Result<(), PluginRuntimeError> {
        if value.len() > self.options.max_cache_value_bytes {
            return Err(host_error("cache value exceeds limit"));
        }
        let path = self.cache_file(key)?;
        fs::create_dir_all(path.parent().unwrap()).map_err(internal)?;
        let existing = directory_size(&self.cache_dir());
        if existing.saturating_add(value.len() as u64) > self.options.max_cache_bytes as u64 {
            return Err(host_error("plugin cache exceeds limit"));
        }
        let tmp = path.with_extension(format!("tmp-{}", now_ms()));
        let expires = if ttl == 0 {
            0
        } else {
            now_ms().saturating_add(ttl)
        };
        fs::write(
            &tmp,
            serde_json::to_vec(&json!({"value":value,"expiresAt":expires})).map_err(internal)?,
        )
        .map_err(internal)?;
        fs::rename(tmp, path).map_err(internal)
    }
    fn http(
        &self,
        method: &str,
        p: &Value,
        binary: bool,
        control: &crate::OperationControl,
    ) -> Result<Value, PluginRuntimeError> {
        let mut url =
            Url::parse(p.get("url").and_then(Value::as_str).unwrap_or("")).map_err(host_error)?;
        let follow = p
            .get("followRedirects")
            .and_then(Value::as_bool)
            .unwrap_or(true);
        let body = request_body(p)?;
        if body.len() > self.options.max_http_request_bytes {
            return Err(host_error("HTTP request body exceeds limit"));
        }
        let connect = p
            .get("connectTimeoutMs")
            .and_then(Value::as_u64)
            .unwrap_or(8000)
            .min(15000);
        let read = p
            .get("readTimeoutMs")
            .and_then(Value::as_u64)
            .unwrap_or(12000)
            .min(30000);
        let client = reqwest::blocking::Client::builder()
            .no_proxy()
            .redirect(reqwest::redirect::Policy::none())
            .connect_timeout(std::time::Duration::from_millis(connect))
            .timeout(std::time::Duration::from_millis(connect + read))
            .build()
            .map_err(host_error)?;
        for redirects in 0..=5 {
            self.validate_url(&url)?;
            let mut request = match method {
                "GET" => client.get(url.clone()),
                _ => client.post(url.clone()).body(body.clone()),
            };
            if let Some(headers) = p.get("headers").and_then(Value::as_object) {
                for (k, v) in headers {
                    let text = if let Some(a) = v.as_array() {
                        a.iter()
                            .filter_map(Value::as_str)
                            .collect::<Vec<_>>()
                            .join(", ")
                    } else {
                        v.as_str().unwrap_or("").to_owned()
                    };
                    if !text.is_empty() {
                        request = request.header(k, text)
                    }
                }
            }
            if p.get("headers")
                .and_then(Value::as_object)
                .map_or(true, |h| {
                    !h.keys().any(|k| k.eq_ignore_ascii_case("user-agent"))
                })
            {
                request = request.header(
                    "User-Agent",
                    format!(
                        "{}/{}",
                        self.options.app_name, self.options.app_version_name
                    ),
                )
            }
            if control.should_interrupt() {
                return Err(control.interrupted_error());
            }
            let mut response = request.send().map_err(host_error)?;
            if control.should_interrupt() {
                return Err(control.interrupted_error());
            }
            if response.status().is_redirection() && follow {
                if redirects == 5 {
                    return Err(host_error("HTTP redirect limit exceeded"));
                }
                let location = response
                    .headers()
                    .get(reqwest::header::LOCATION)
                    .ok_or_else(|| host_error("redirect missing Location"))?
                    .to_str()
                    .map_err(host_error)?;
                url = url.join(location).map_err(host_error)?;
                continue;
            }
            let code = response.status().as_u16();
            let message = response
                .status()
                .canonical_reason()
                .unwrap_or("")
                .to_owned();
            let mut headers = Map::new();
            for name in response.headers().keys() {
                let values = response
                    .headers()
                    .get_all(name)
                    .iter()
                    .filter_map(|x| x.to_str().ok())
                    .map(|x| Value::String(x.to_owned()))
                    .collect();
                headers.insert(name.to_string(), Value::Array(values));
            }
            let mut data = Vec::new();
            let mut chunk = [0u8; 8192];
            loop {
                if control.should_interrupt() {
                    return Err(control.interrupted_error());
                }
                let count = response.read(&mut chunk).map_err(host_error)?;
                if count == 0 {
                    break;
                }
                data.extend_from_slice(&chunk[..count]);
                if data.len() > self.options.max_http_response_bytes {
                    return Err(host_error("HTTP response body exceeds limit"));
                }
            }
            if data.len() > self.options.max_http_response_bytes {
                return Err(host_error("HTTP response body exceeds limit"));
            }
            return Ok(
                json!({"code":code,"message":message,"headers":headers,"body":if binary{"".into()}else{String::from_utf8_lossy(&data).into_owned()},"bodyBase64":if binary{STANDARD.encode(data)}else{"".into()}}),
            );
        }
        Err(host_error("HTTP redirect failed"))
    }
    fn validate_url(&self, url: &Url) -> Result<(), PluginRuntimeError> {
        match url.scheme() {
            "https" if self.options.allow_https => {}
            "http" if self.options.allow_http => {}
            "http" | "https" => return Err(host_error("HTTP scheme disabled")),
            _ => return Err(host_error("unsupported URL scheme")),
        }
        let host = url
            .host_str()
            .ok_or_else(|| host_error("URL host missing"))?;
        let port = url
            .port_or_known_default()
            .ok_or_else(|| host_error("URL port missing"))?;
        let addresses = (host, port)
            .to_socket_addrs()
            .map_err(host_error)?
            .collect::<Vec<_>>();
        if addresses.is_empty() {
            return Err(host_error("DNS resolution returned no addresses"));
        }
        if !self.options.allow_private_network && addresses.iter().any(|a| blocked_ip(a.ip())) {
            return Err(host_error("private network target blocked"));
        }
        Ok(())
    }
}
impl HostApiDispatcher for HostApi {
    fn call(
        &mut self,
        name: &str,
        payload_json: &str,
        control: &crate::OperationControl,
    ) -> Result<String, PluginRuntimeError> {
        let p: Value = serde_json::from_str(payload_json).map_err(|e| host_error(e.to_string()))?;
        let s = |k: &str| p.get(k).and_then(Value::as_str).unwrap_or("");
        let bytes = |k: &str| json_bytes(p.get(k));
        match name {
            "app.info" => Self::value(
                json!({"name":self.options.app_name,"packageName":self.options.package_name,"versionName":self.options.app_version_name,"versionCode":self.options.app_version_code,"buildType":self.options.build_type,"debug":self.options.debug}),
            ),
            "app.userAgent" => Self::value(json!(format!(
                "{}/{}",
                self.options.app_name, self.options.app_version_name
            ))),
            "runtime.info" => Self::value(
                json!({"pluginApiVersion":3,"hostApiVersion":3,"engine":"quickjs","engineVersion":"quickjs-ng 0.14.0","supportedHostApis":SUPPORTED_HOST_APIS}),
            ),
            "cache.get" => Self::value(json!(self.cache_get(s("key")))),
            "cache.set" => {
                self.cache_set(
                    s("key"),
                    s("value"),
                    p.get("ttlMs").and_then(Value::as_u64).unwrap_or(0),
                )?;
                Self::value(json!(""))
            }
            "cache.remove" => {
                let _ = fs::remove_file(self.cache_file(s("key"))?);
                Self::value(json!(""))
            }
            "cache.clear" => {
                let dir = self.cache_dir();
                if dir.starts_with(self.options.cache_directory.join("plugins")) {
                    let _ = fs::remove_dir_all(dir);
                }
                Self::value(json!(""))
            }
            "crypto.md5" => Self::value(json!(format!("{:x}", Md5::digest(s("text").as_bytes())))),
            "crypto.aesEcbPkcs5EncryptBase64" => Self::value(json!(
                STANDARD.encode(aes_encrypt(s("text").as_bytes(), s("key").as_bytes())?)
            )),
            "crypto.aesEcbPkcs5EncryptHex" => Self::value(json!(hex_upper(&aes_encrypt(
                s("text").as_bytes(),
                s("key").as_bytes()
            )?))),
            "crypto.aesEcbPkcs5DecryptBase64ToText" => {
                Self::value(json!(String::from_utf8(aes_decrypt(
                    &STANDARD.decode(s("base64")).map_err(host_error)?,
                    s("key").as_bytes()
                )?)
                .map_err(host_error)?))
            }
            "base64.encodeText" => Self::value(json!(STANDARD.encode(s("text")))),
            "base64.decodeText" => Self::value(json!(String::from_utf8(
                STANDARD.decode(s("base64")).map_err(host_error)?
            )
            .map_err(host_error)?)),
            "base64.dropBytes" => {
                let b = STANDARD.decode(s("base64")).map_err(host_error)?;
                Self::value(json!(STANDARD.encode(
                    &b[p.get("count")
                        .and_then(Value::as_u64)
                        .unwrap_or(0)
                        .min(b.len() as u64) as usize..]
                )))
            }
            "base64.decodeBytes" => {
                Self::value(json!(STANDARD.decode(s("base64")).map_err(host_error)?))
            }
            "base64.encodeBytes" => Self::value(json!(STANDARD.encode(bytes("bytes")?))),
            "base64.encodeUrlText" => Self::value(json!(URL_SAFE_NO_PAD.encode(s("text")))),
            "base64.decodeUrlText" => Self::value(json!(String::from_utf8(decode_url(s(
                "base64Url"
            ))?)
            .map_err(host_error)?)),
            "base64.encodeUrlBytes" => Self::value(json!(URL_SAFE_NO_PAD.encode(bytes("bytes")?))),
            "base64.decodeUrlBytes" => Self::value(json!(decode_url(s("base64Url"))?)),
            "base64.toUrl" => Self::value(json!(s("base64")
                .trim()
                .replace('+', "-")
                .replace('/', "_")
                .trim_end_matches('='))),
            "base64.fromUrl" => Self::value(json!(to_standard_url(s("base64Url")))),
            "bytes.xor" => Self::value(json!(xor(bytes("bytes")?, bytes("key")?))),
            "bytes.xorBase64" => Self::value(json!(STANDARD.encode(xor(
                STANDARD.decode(s("base64")).map_err(host_error)?,
                bytes("key")?
            )))),
            "compression.inflateBytesToText" => Self::value(json!(inflate(
                &bytes("bytes")?,
                self.options.max_inflate_bytes
            )?)),
            "compression.inflateBase64ToText" => Self::value(json!(inflate(
                &STANDARD.decode(s("base64")).map_err(host_error)?,
                self.options.max_inflate_bytes
            )?)),
            "http.getText" | "http.get" | "http.getBytes" => {
                let binary = name == "http.getBytes";
                let response = self.http("GET", &p, binary, control)?;
                if name == "http.getText" {
                    Self::value(
                        response
                            .get("body")
                            .cloned()
                            .unwrap_or(Value::String(String::new())),
                    )
                } else {
                    Self::value(response)
                }
            }
            "http.postText" | "http.postBytes" | "http.post" | "http.postBytesResponse" => {
                let binary = matches!(name, "http.postBytes" | "http.postBytesResponse");
                let response = self.http("POST", &p, binary, control)?;
                if name == "http.postText" {
                    Self::value(
                        response
                            .get("body")
                            .cloned()
                            .unwrap_or(Value::String(String::new())),
                    )
                } else if name == "http.postBytes" {
                    Self::value(
                        response
                            .get("bodyBase64")
                            .cloned()
                            .unwrap_or(Value::String(String::new())),
                    )
                } else {
                    Self::value(response)
                }
            }
            "log.debug" | "log.warn" | "log.error" => {
                let tag = s("tag").chars().take(48).collect::<String>();
                eprintln!(
                    "plugin={} level={} tag={} message={}",
                    self.options.plugin_id,
                    name,
                    tag,
                    s("message")
                );
                Self::value(json!(""))
            }
            "xml.getRootAttributes" => Self::value(xml::root_attributes(s("xml"))?),
            "xml.findElements" => Self::value(xml::find_elements(
                s("xml"),
                p.get("query").unwrap_or(&Value::Null),
            )?),
            "xml.replaceChildrenByAttr" => Self::value(json!(xml::replace_children(
                s("xml"),
                p.get("options").unwrap_or(&Value::Null)
            )?)),
            "xml.removeElements" => Self::value(json!(xml::remove_elements(
                s("xml"),
                p.get("query").unwrap_or(&Value::Null)
            )?)),
            _ => Err(host_error(format!("unsupported API: {name}"))),
        }
    }
}
fn json_bytes(v: Option<&Value>) -> Result<Vec<u8>, PluginRuntimeError> {
    v.and_then(Value::as_array)
        .unwrap_or(&vec![])
        .iter()
        .map(|x| {
            x.as_u64()
                .filter(|x| *x <= 255)
                .map(|x| x as u8)
                .ok_or_else(|| host_error("byte must be 0..255"))
        })
        .collect()
}
fn request_body(p: &Value) -> Result<Vec<u8>, PluginRuntimeError> {
    if let Some(s) = p
        .get("bodyBase64")
        .and_then(Value::as_str)
        .filter(|s| !s.is_empty())
    {
        return STANDARD.decode(s).map_err(host_error);
    }
    if p.get("bodyBytes").is_some() {
        return json_bytes(p.get("bodyBytes"));
    }
    Ok(p.get("body")
        .and_then(Value::as_str)
        .unwrap_or("")
        .as_bytes()
        .to_vec())
}
fn blocked_ip(ip: IpAddr) -> bool {
    match ip {
        IpAddr::V4(x) => {
            x.is_private()
                || x.is_loopback()
                || x.is_link_local()
                || x.is_broadcast()
                || x.is_unspecified()
                || x.is_multicast()
                || x.octets()[0] == 0
        }
        IpAddr::V6(x) => {
            x.is_loopback()
                || x.is_unspecified()
                || x.is_multicast()
                || (x.segments()[0] & 0xfe00) == 0xfc00
                || (x.segments()[0] & 0xffc0) == 0xfe80
        }
    }
}
fn aes_encrypt(data: &[u8], key: &[u8]) -> Result<Vec<u8>, PluginRuntimeError> {
    let mut b = vec![0; data.len() + 16];
    b[..data.len()].copy_from_slice(data);
    macro_rules! enc {
        ($t:ty) => {
            <$t>::new_from_slice(key)
                .map_err(host_error)?
                .encrypt_padded_mut::<Pkcs7>(&mut b, data.len())
                .map_err(host_error)?
                .to_vec()
        };
    }
    match key.len() {
        16 => Ok(enc!(Aes128)),
        24 => Ok(enc!(Aes192)),
        32 => Ok(enc!(Aes256)),
        _ => Err(host_error("AES key must be 16, 24, or 32 bytes")),
    }
}
fn aes_decrypt(data: &[u8], key: &[u8]) -> Result<Vec<u8>, PluginRuntimeError> {
    let mut b = data.to_vec();
    macro_rules! dec {
        ($t:ty) => {
            <$t>::new_from_slice(key)
                .map_err(host_error)?
                .decrypt_padded_mut::<Pkcs7>(&mut b)
                .map_err(host_error)?
                .to_vec()
        };
    }
    match key.len() {
        16 => Ok(dec!(Aes128)),
        24 => Ok(dec!(Aes192)),
        32 => Ok(dec!(Aes256)),
        _ => Err(host_error("AES key must be 16, 24, or 32 bytes")),
    }
}
fn xor(mut b: Vec<u8>, k: Vec<u8>) -> Vec<u8> {
    if !k.is_empty() {
        for (i, x) in b.iter_mut().enumerate() {
            *x ^= k[i % k.len()]
        }
    }
    b
}
fn inflate(b: &[u8], limit: usize) -> Result<String, PluginRuntimeError> {
    let d = ZlibDecoder::new(b);
    let mut out = Vec::new();
    d.take((limit + 1) as u64)
        .read_to_end(&mut out)
        .map_err(host_error)?;
    if out.len() > limit {
        return Err(host_error("inflated output exceeds limit"));
    }
    String::from_utf8(out).map_err(host_error)
}
fn decode_url(s: &str) -> Result<Vec<u8>, PluginRuntimeError> {
    URL_SAFE_NO_PAD
        .decode(s.trim().trim_end_matches('='))
        .map_err(host_error)
}
fn to_standard_url(s: &str) -> String {
    let mut x = s.trim().replace('-', "+").replace('_', "/");
    while x.len() % 4 != 0 {
        x.push('=')
    }
    x
}
fn hex_upper(b: &[u8]) -> String {
    b.iter().map(|x| format!("{x:02x}")).collect()
}
fn sha256_hex(b: &[u8]) -> String {
    Sha256::digest(b)
        .iter()
        .map(|x| format!("{x:02x}"))
        .collect()
}
fn now_ms() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64
}
fn directory_size(p: &Path) -> u64 {
    fs::read_dir(p)
        .into_iter()
        .flatten()
        .flatten()
        .filter_map(|x| x.metadata().ok())
        .filter(|m| m.is_file())
        .map(|m| m.len())
        .sum()
}
fn host_error(e: impl ToString) -> PluginRuntimeError {
    PluginRuntimeError::HostApi(e.to_string())
}
fn internal(e: impl ToString) -> PluginRuntimeError {
    PluginRuntimeError::Internal(e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::OperationControl;
    use flate2::{write::ZlibEncoder, Compression};
    use std::io::Write;

    fn host(id: &str) -> HostApi {
        let root = std::env::temp_dir().join(format!("tidetunes-host-test-{}-{id}", now_ms()));
        HostApi::new(HostApiOptions {
            plugin_id: id.into(),
            cache_directory: root,
            ..Default::default()
        })
    }
    fn value(host: &mut HostApi, name: &str, payload: Value) -> Value {
        let response = host
            .call(name, &payload.to_string(), &OperationControl::default())
            .unwrap();
        serde_json::from_str::<Value>(&response).unwrap()["value"].clone()
    }
    #[test]
    fn crypto_and_encoding_golden() {
        let mut h = host("crypto");
        assert_eq!(
            value(&mut h, "crypto.md5", json!({"text":"abc"})),
            "900150983cd24fb0d6963f7d28e17f72"
        );
        let encrypted = value(
            &mut h,
            "crypto.aesEcbPkcs5EncryptBase64",
            json!({"text":"hello","key":"1234567890123456"}),
        );
        assert_eq!(
            value(
                &mut h,
                "crypto.aesEcbPkcs5DecryptBase64ToText",
                json!({"base64":encrypted,"key":"1234567890123456"})
            ),
            "hello"
        );
        assert_eq!(
            value(&mut h, "base64.encodeUrlText", json!({"text":"你好?"})),
            URL_SAFE_NO_PAD.encode("你好?")
        );
        assert_eq!(
            value(&mut h, "bytes.xor", json!({"bytes":[1,2,3],"key":[255]})),
            json!([254, 253, 252])
        );
        assert!(h
            .call(
                "base64.encodeBytes",
                r#"{"bytes":[256]}"#,
                &OperationControl::default()
            )
            .is_err());
    }
    #[test]
    fn inflate_is_bounded() {
        let mut compressed = ZlibEncoder::new(Vec::new(), Compression::default());
        compressed.write_all(b"hello").unwrap();
        let bytes = compressed.finish().unwrap();
        let mut h = host("inflate");
        assert_eq!(
            value(
                &mut h,
                "compression.inflateBytesToText",
                json!({"bytes":bytes})
            ),
            "hello"
        );
        h.options.max_inflate_bytes = 4;
        assert!(h
            .call(
                "compression.inflateBytesToText",
                &json!({"bytes":bytes}).to_string(),
                &OperationControl::default()
            )
            .is_err());
    }
    #[test]
    fn cache_ttl_and_isolation() {
        let root = std::env::temp_dir().join(format!("tidetunes-cache-test-{}", now_ms()));
        let mut a = HostApi::new(HostApiOptions {
            plugin_id: "a".into(),
            cache_directory: root.clone(),
            ..Default::default()
        });
        let mut b = HostApi::new(HostApiOptions {
            plugin_id: "b".into(),
            cache_directory: root.clone(),
            ..Default::default()
        });
        value(
            &mut a,
            "cache.set",
            json!({"key":"k","value":"secret","ttlMs":1000}),
        );
        assert_eq!(value(&mut a, "cache.get", json!({"key":"k"})), "secret");
        assert_eq!(value(&mut b, "cache.get", json!({"key":"k"})), "");
        value(
            &mut a,
            "cache.set",
            json!({"key":"expired","value":"x","ttlMs":1}),
        );
        std::thread::sleep(std::time::Duration::from_millis(3));
        assert_eq!(value(&mut a, "cache.get", json!({"key":"expired"})), "");
        let _ = fs::remove_dir_all(root);
    }
    #[test]
    fn http_blocks_unsafe_targets_by_default() {
        let mut h = host("http");
        assert!(h
            .call(
                "http.get",
                r#"{"url":"file:///etc/passwd"}"#,
                &OperationControl::default()
            )
            .is_err());
        assert!(h
            .call(
                "http.get",
                r#"{"url":"http://127.0.0.1/"}"#,
                &OperationControl::default()
            )
            .is_err());
        assert!(h
            .call(
                "http.get",
                r#"{"url":"https://localhost/"}"#,
                &OperationControl::default()
            )
            .is_err());
    }
    #[test]
    fn http_text_binary_and_size_limit() {
        use std::{
            io::{Read, Write},
            net::TcpListener,
            thread,
        };
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let addr = listener.local_addr().unwrap();
        thread::spawn(move || {
            for _ in 0..3 {
                let (mut s, _) = listener.accept().unwrap();
                let mut b = [0u8; 2048];
                let _ = s.read(&mut b);
                s.write_all(b"HTTP/1.1 200 OK\r\nContent-Length: 5\r\nSet-Cookie: a=b\r\nConnection: close\r\n\r\nhello").unwrap();
            }
        });
        let mut h = host("http-live");
        h.options.allow_http = true;
        h.options.allow_private_network = true;
        let url = format!("http://{addr}/");
        assert_eq!(value(&mut h, "http.getText", json!({"url":url})), "hello");
        let binary = value(&mut h, "http.getBytes", json!({"url":url}));
        assert_eq!(binary["bodyBase64"], STANDARD.encode("hello"));
        assert_eq!(binary["headers"]["set-cookie"][0], "a=b");
        h.options.max_http_response_bytes = 4;
        assert!(h
            .call(
                "http.get",
                &json!({"url":url}).to_string(),
                &OperationControl::default()
            )
            .is_err());
    }
}
