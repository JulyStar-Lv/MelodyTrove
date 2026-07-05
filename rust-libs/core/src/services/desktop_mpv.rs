use std::{
    ffi::{CStr, CString},
    os::raw::{c_char, c_double, c_int, c_void},
    sync::{Arc, Mutex},
};

use libloading::Library;

const MPV_FORMAT_DOUBLE: c_int = 5;

type MpvCreate = unsafe extern "C" fn() -> *mut c_void;
type MpvInitialize = unsafe extern "C" fn(*mut c_void) -> c_int;
type MpvTerminateDestroy = unsafe extern "C" fn(*mut c_void);
type MpvCommand = unsafe extern "C" fn(*mut c_void, *const *const c_char) -> c_int;
type MpvCommandString = unsafe extern "C" fn(*mut c_void, *const c_char) -> c_int;
type MpvGetProperty = unsafe extern "C" fn(*mut c_void, *const c_char, c_int, *mut c_void) -> c_int;
type MpvSetOptionString = unsafe extern "C" fn(*mut c_void, *const c_char, *const c_char) -> c_int;
type MpvErrorString = unsafe extern "C" fn(c_int) -> *const c_char;

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum DesktopMpvLoadResult {
    Ready,
    Unsupported,
}

#[derive(uniffi::Object)]
pub struct DesktopMpvPlayer {
    state: Mutex<DesktopMpvState>,
}

#[derive(Default)]
struct DesktopMpvState {
    mpv: Option<LoadedMpv>,
    loaded: bool,
}

#[uniffi::export]
impl DesktopMpvPlayer {
    pub fn load(&self, uri: String, http_header_fields: String) -> DesktopMpvLoadResult {
        if uri.trim().is_empty() {
            return DesktopMpvLoadResult::Unsupported;
        }

        let mut state = self.state.lock().unwrap();
        let mpv = match state.ensure_mpv() {
            Ok(mpv) => mpv,
            Err(message) => {
                tracing::warn!(message, "desktop mpv unavailable");
                return DesktopMpvLoadResult::Unsupported;
            }
        };

        match mpv.load(&uri, &http_header_fields) {
            Ok(()) => {
                state.loaded = true;
                DesktopMpvLoadResult::Ready
            }
            Err(message) => {
                tracing::warn!(message, "desktop mpv failed to load resource");
                state.loaded = false;
                DesktopMpvLoadResult::Unsupported
            }
        }
    }

    pub fn play(&self) {
        self.with_loaded_mpv(|mpv| {
            let _ = mpv.command_string("set pause no");
        });
    }

    pub fn pause(&self) {
        self.with_loaded_mpv(|mpv| {
            let _ = mpv.command_string("set pause yes");
        });
    }

    pub fn stop(&self) {
        let mut state = self.state.lock().unwrap();
        if let Some(mpv) = state.mpv.as_ref() {
            let _ = mpv.command_string("stop");
        }
        state.loaded = false;
    }

    pub fn seek(&self, ms: u64) {
        self.with_loaded_mpv(|mpv| {
            let seconds = format!("{:.3}", ms as f64 / 1000.0);
            let _ = mpv.command(&["seek", &seconds, "absolute"]);
        });
    }

    pub fn current_position_ms(&self) -> i64 {
        self.property_seconds_ms("time-pos")
    }

    pub fn buffered_position_ms(&self) -> i64 {
        let current = self.current_position_ms();
        let buffered = self.property_seconds_ms("demuxer-cache-time");
        buffered.max(current)
    }

    pub fn duration_ms(&self) -> i64 {
        self.property_seconds_ms("duration")
    }
}

impl DesktopMpvPlayer {
    fn new() -> Self {
        Self {
            state: Mutex::new(DesktopMpvState::default()),
        }
    }

    fn with_loaded_mpv(&self, block: impl FnOnce(&LoadedMpv)) {
        let state = self.state.lock().unwrap();
        if !state.loaded {
            return;
        }
        if let Some(mpv) = state.mpv.as_ref() {
            block(mpv);
        }
    }

    fn property_seconds_ms(&self, name: &str) -> i64 {
        let state = self.state.lock().unwrap();
        if !state.loaded {
            return 0;
        }
        state
            .mpv
            .as_ref()
            .and_then(|mpv| mpv.property_double(name).ok())
            .map(|seconds| (seconds.max(0.0) * 1000.0) as i64)
            .unwrap_or(0)
    }
}

impl DesktopMpvState {
    fn ensure_mpv(&mut self) -> Result<&LoadedMpv, String> {
        if self.mpv.is_none() {
            self.mpv = Some(LoadedMpv::new()?);
        }
        Ok(self.mpv.as_ref().unwrap())
    }
}

#[uniffi::export]
pub fn ct_create_desktop_mpv_player() -> Arc<DesktopMpvPlayer> {
    Arc::new(DesktopMpvPlayer::new())
}

struct LoadedMpv {
    library: Arc<MpvLibrary>,
    handle: *mut c_void,
}

unsafe impl Send for LoadedMpv {}

impl LoadedMpv {
    fn new() -> Result<Self, String> {
        let library = Arc::new(MpvLibrary::load()?);
        let handle = unsafe { (library.create)() };
        if handle.is_null() {
            return Err("mpv_create returned null".to_string());
        }

        let mpv = Self { library, handle };
        mpv.set_option("terminal", "no")?;
        mpv.set_option("input-terminal", "no")?;
        mpv.set_option("audio-display", "no")?;
        mpv.set_option("video", "no")?;
        mpv.initialize()?;
        Ok(mpv)
    }

    fn initialize(&self) -> Result<(), String> {
        let code = unsafe { (self.library.initialize)(self.handle) };
        self.check(code)
    }

    fn load(&self, uri: &str, http_header_fields: &str) -> Result<(), String> {
        self.set_http_header_fields(http_header_fields)?;
        self.command(&["loadfile", uri, "replace"])
    }

    fn command(&self, args: &[&str]) -> Result<(), String> {
        let strings = args
            .iter()
            .map(|arg| CString::new(*arg).map_err(|_| format!("mpv command contains NUL: {arg}")))
            .collect::<Result<Vec<_>, _>>()?;
        let mut pointers = strings.iter().map(|arg| arg.as_ptr()).collect::<Vec<_>>();
        pointers.push(std::ptr::null());

        let code = unsafe { (self.library.command)(self.handle, pointers.as_ptr()) };
        self.check(code)
    }

    fn command_string(&self, command: &str) -> Result<(), String> {
        let command = CString::new(command)
            .map_err(|_| format!("mpv command string contains NUL: {command}"))?;
        let code = unsafe { (self.library.command_string)(self.handle, command.as_ptr()) };
        self.check(code)
    }

    fn set_option(&self, name: &str, value: &str) -> Result<(), String> {
        let name = CString::new(name).map_err(|_| format!("mpv option contains NUL: {name}"))?;
        let value =
            CString::new(value).map_err(|_| format!("mpv option value contains NUL: {value}"))?;
        let code =
            unsafe { (self.library.set_option_string)(self.handle, name.as_ptr(), value.as_ptr()) };
        self.check(code)
    }

    fn set_http_header_fields(&self, http_header_fields: &str) -> Result<(), String> {
        let value = http_header_fields
            .lines()
            .map(str::trim)
            .filter(|line| !line.is_empty())
            .collect::<Vec<_>>()
            .join(",");
        self.command(&["set", "http-header-fields", &value])
    }

    fn property_double(&self, name: &str) -> Result<f64, String> {
        let name = CString::new(name).map_err(|_| format!("mpv property contains NUL: {name}"))?;
        let mut value: c_double = 0.0;
        let code = unsafe {
            (self.library.get_property)(
                self.handle,
                name.as_ptr(),
                MPV_FORMAT_DOUBLE,
                (&mut value as *mut c_double).cast::<c_void>(),
            )
        };
        self.check(code)?;
        Ok(value)
    }

    fn check(&self, code: c_int) -> Result<(), String> {
        if code >= 0 {
            return Ok(());
        }
        Err(self.library.error_message(code))
    }
}

impl Drop for LoadedMpv {
    fn drop(&mut self) {
        unsafe { (self.library.terminate_destroy)(self.handle) };
    }
}

struct MpvLibrary {
    _library: Library,
    create: MpvCreate,
    initialize: MpvInitialize,
    terminate_destroy: MpvTerminateDestroy,
    command: MpvCommand,
    command_string: MpvCommandString,
    get_property: MpvGetProperty,
    set_option_string: MpvSetOptionString,
    error_string: MpvErrorString,
}

unsafe impl Send for MpvLibrary {}
unsafe impl Sync for MpvLibrary {}

impl MpvLibrary {
    fn load() -> Result<Self, String> {
        let mut errors = Vec::new();
        for candidate in mpv_library_candidates() {
            match unsafe { Library::new(&candidate) } {
                Ok(library) => return unsafe { Self::from_library(library) },
                Err(error) => errors.push(format!("{candidate}: {error}")),
            }
        }
        Err(format!(
            "libmpv was not found. Set TIDETUNES_MPV_LIBRARY or install libmpv. Tried: {}",
            errors.join("; ")
        ))
    }

    unsafe fn from_library(library: Library) -> Result<Self, String> {
        let create = *library
            .get::<MpvCreate>(b"mpv_create\0")
            .map_err(|error| format!("missing mpv_create: {error}"))?;
        let initialize = *library
            .get::<MpvInitialize>(b"mpv_initialize\0")
            .map_err(|error| format!("missing mpv_initialize: {error}"))?;
        let terminate_destroy = *library
            .get::<MpvTerminateDestroy>(b"mpv_terminate_destroy\0")
            .map_err(|error| format!("missing mpv_terminate_destroy: {error}"))?;
        let command = *library
            .get::<MpvCommand>(b"mpv_command\0")
            .map_err(|error| format!("missing mpv_command: {error}"))?;
        let command_string = *library
            .get::<MpvCommandString>(b"mpv_command_string\0")
            .map_err(|error| format!("missing mpv_command_string: {error}"))?;
        let get_property = *library
            .get::<MpvGetProperty>(b"mpv_get_property\0")
            .map_err(|error| format!("missing mpv_get_property: {error}"))?;
        let set_option_string = *library
            .get::<MpvSetOptionString>(b"mpv_set_option_string\0")
            .map_err(|error| format!("missing mpv_set_option_string: {error}"))?;
        let error_string = *library
            .get::<MpvErrorString>(b"mpv_error_string\0")
            .map_err(|error| format!("missing mpv_error_string: {error}"))?;
        Ok(Self {
            _library: library,
            create,
            initialize,
            terminate_destroy,
            command,
            command_string,
            get_property,
            set_option_string,
            error_string,
        })
    }

    fn error_message(&self, code: c_int) -> String {
        let message = unsafe { (self.error_string)(code) };
        if message.is_null() {
            return format!("mpv error {code}");
        }
        unsafe { CStr::from_ptr(message) }
            .to_string_lossy()
            .into_owned()
    }
}

fn mpv_library_candidates() -> Vec<String> {
    let mut candidates = Vec::new();
    if let Ok(path) = std::env::var("TIDETUNES_MPV_LIBRARY") {
        if !path.trim().is_empty() {
            candidates.push(path);
        }
    }

    #[cfg(target_os = "macos")]
    {
        candidates.extend([
            "libmpv.2.dylib".to_string(),
            "libmpv.dylib".to_string(),
            "/opt/homebrew/lib/libmpv.2.dylib".to_string(),
            "/opt/homebrew/lib/libmpv.dylib".to_string(),
            "/usr/local/lib/libmpv.2.dylib".to_string(),
            "/usr/local/lib/libmpv.dylib".to_string(),
        ]);
    }

    #[cfg(target_os = "linux")]
    {
        candidates.extend(["libmpv.so.2".to_string(), "libmpv.so".to_string()]);
    }

    #[cfg(target_os = "windows")]
    {
        candidates.extend([
            "mpv-2.dll".to_string(),
            "libmpv-2.dll".to_string(),
            "mpv-1.dll".to_string(),
            "libmpv.dll".to_string(),
        ]);
    }

    #[cfg(not(any(target_os = "macos", target_os = "linux", target_os = "windows")))]
    {
        candidates.push("libmpv.so".to_string());
    }

    candidates
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_uri_is_unsupported_without_loading_mpv() {
        let player = DesktopMpvPlayer::new();

        assert_eq!(
            DesktopMpvLoadResult::Unsupported,
            player.load("".to_string(), "".to_string())
        );
        player.play();
        player.pause();
        player.seek(1_000);
        player.stop();
        assert_eq!(0, player.current_position_ms());
        assert_eq!(0, player.buffered_position_ms());
        assert_eq!(0, player.duration_ms());
    }
}
