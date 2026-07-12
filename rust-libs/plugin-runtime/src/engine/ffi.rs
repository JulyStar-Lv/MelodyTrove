use std::ffi::{c_char, c_int, c_void};
#[repr(C)]
pub struct TTQuickJs {
    _private: [u8; 0],
}
#[repr(C)]
#[derive(Clone, Copy)]
pub struct StringView {
    pub data: *const c_char,
    pub length: usize,
}
pub type HostCall = unsafe extern "C" fn(
    *mut c_void,
    StringView,
    StringView,
    *mut StringView,
    *mut StringView,
) -> c_int;
pub type Interrupt = unsafe extern "C" fn(*mut c_void) -> c_int;
#[repr(C)]
pub struct Options {
    pub memory_limit_bytes: u64,
    pub stack_limit_bytes: u64,
    pub host_call: Option<HostCall>,
    pub should_interrupt: Option<Interrupt>,
    pub opaque: *mut c_void,
}
extern "C" {
    pub fn tt_qjs_create(o: *const Options, e: *mut *mut c_char) -> *mut TTQuickJs;
    pub fn tt_qjs_eval(
        q: *mut TTQuickJs,
        s: *const c_char,
        n: usize,
        f: *const c_char,
        r: *mut *mut c_char,
        e: *mut *mut c_char,
    ) -> c_int;
    pub fn tt_qjs_call_json(
        q: *mut TTQuickJs,
        f: *const c_char,
        j: *const c_char,
        n: usize,
        r: *mut *mut c_char,
        e: *mut *mut c_char,
    ) -> c_int;
    pub fn tt_qjs_free_string(v: *mut c_char);
    pub fn tt_qjs_destroy(q: *mut TTQuickJs);
}
