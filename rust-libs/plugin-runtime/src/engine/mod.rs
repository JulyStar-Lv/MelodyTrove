mod ffi;
use crate::{HostApiDispatcher, OperationControl, PluginRuntimeError};
use std::{
    ffi::{c_char, c_int, c_void, CStr, CString},
    ptr,
    sync::Arc,
};
struct CallbackState {
    control: Arc<OperationControl>,
    host: Box<dyn HostApiDispatcher>,
    result: Vec<u8>,
    error: Vec<u8>,
}
pub struct QuickJsEngine {
    raw: *mut ffi::TTQuickJs,
    state: Box<CallbackState>,
}
unsafe extern "C" fn interrupt(p: *mut c_void) -> c_int {
    let s = &*(p as *mut CallbackState);
    s.control.should_interrupt() as c_int
}
unsafe extern "C" fn host(
    p: *mut c_void,
    n: ffi::StringView,
    v: ffi::StringView,
    r: *mut ffi::StringView,
    e: *mut ffi::StringView,
) -> c_int {
    let s = &mut *(p as *mut CallbackState);
    let name = std::str::from_utf8(std::slice::from_raw_parts(n.data as *const u8, n.length))
        .unwrap_or("");
    let payload = std::str::from_utf8(std::slice::from_raw_parts(v.data as *const u8, v.length))
        .unwrap_or("");
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        s.host.call(name, payload, &s.control)
    })) {
        Ok(Ok(x)) => {
            s.result = x.into_bytes();
            *r = ffi::StringView {
                data: s.result.as_ptr() as _,
                length: s.result.len(),
            };
            0
        }
        Ok(Err(x)) => {
            s.error = x.to_string().into_bytes();
            *e = ffi::StringView {
                data: s.error.as_ptr() as _,
                length: s.error.len(),
            };
            1
        }
        Err(_) => {
            s.control
                .poisoned
                .store(true, std::sync::atomic::Ordering::Release);
            s.error = b"host callback panic".to_vec();
            *e = ffi::StringView {
                data: s.error.as_ptr() as _,
                length: s.error.len(),
            };
            1
        }
    }
}
impl QuickJsEngine {
    pub fn new(
        memory: u64,
        stack: u64,
        control: Arc<OperationControl>,
        host_api: Box<dyn HostApiDispatcher>,
    ) -> Result<Self, PluginRuntimeError> {
        let mut state = Box::new(CallbackState {
            control,
            host: host_api,
            result: vec![],
            error: vec![],
        });
        let o = ffi::Options {
            memory_limit_bytes: memory,
            stack_limit_bytes: stack,
            host_call: Some(host),
            should_interrupt: Some(interrupt),
            opaque: &mut *state as *mut _ as _,
        };
        let mut error = ptr::null_mut();
        let raw = unsafe { ffi::tt_qjs_create(&o, &mut error) };
        if raw.is_null() {
            return Err(PluginRuntimeError::Initialization(take(error)));
        }
        Ok(Self { raw, state })
    }
    pub fn eval(&mut self, script: &str, filename: &str) -> Result<(), PluginRuntimeError> {
        let script_c =
            CString::new(script).map_err(|e| PluginRuntimeError::Script(e.to_string()))?;
        let f = CString::new(filename).map_err(|e| PluginRuntimeError::Script(e.to_string()))?;
        let raw = self.raw;
        self.invoke(|r, e| unsafe {
            ffi::tt_qjs_eval(raw, script_c.as_ptr(), script.len(), f.as_ptr(), r, e)
        })
        .map(|_| ())
    }
    pub fn call(&mut self, name: &str, json: &str) -> Result<String, PluginRuntimeError> {
        let json_c =
            CString::new(json).map_err(|e| PluginRuntimeError::InvalidRequest(e.to_string()))?;
        let n =
            CString::new(name).map_err(|e| PluginRuntimeError::InvalidRequest(e.to_string()))?;
        let raw = self.raw;
        self.invoke(|r, e| unsafe {
            ffi::tt_qjs_call_json(raw, n.as_ptr(), json_c.as_ptr(), json.len(), r, e)
        })
    }
    fn invoke(
        &mut self,
        f: impl FnOnce(*mut *mut c_char, *mut *mut c_char) -> c_int,
    ) -> Result<String, PluginRuntimeError> {
        let (mut r, mut e) = (ptr::null_mut(), ptr::null_mut());
        let code = f(&mut r, &mut e);
        let rs = take(r);
        let es = take(e);
        if code == 0 {
            return Ok(rs);
        };
        if self.state.control.should_interrupt() {
            return Err(self.state.control.interrupted_error());
        }
        match code {
            2 => Err(PluginRuntimeError::FunctionNotFound(es)),
            3 => Err(PluginRuntimeError::InvalidRequest(es)),
            _ => Err(classify(es)),
        }
    }
}
fn take(p: *mut c_char) -> String {
    if p.is_null() {
        return String::new();
    }
    let s = unsafe { CStr::from_ptr(p) }.to_string_lossy().into_owned();
    unsafe { ffi::tt_qjs_free_string(p) };
    s
}
fn classify(s: String) -> PluginRuntimeError {
    let l = s.to_lowercase();
    if l.contains("out of memory") {
        PluginRuntimeError::OutOfMemory
    } else if l.contains("host api") {
        PluginRuntimeError::HostApi(s)
    } else {
        PluginRuntimeError::Script(s)
    }
}
impl Drop for QuickJsEngine {
    fn drop(&mut self) {
        unsafe { ffi::tt_qjs_destroy(self.raw) }
    }
}
