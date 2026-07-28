fn main() {
    let vendor = "vendor/quickjs-ng";
    cc::Build::new()
        .std("c11")
        // quickjs-ng uses POSIX clocks and the GNU/BSD tm_gmtoff field on
        // Unix-like targets. Strict C11 mode hides those declarations on
        // glibc unless GNU feature extensions are enabled before headers are
        // included.
        .define("_GNU_SOURCE", None)
        .define("CONFIG_VERSION", "\"0.14.0\"")
        .include(vendor)
        .include("src/engine")
        .files([
            format!("{vendor}/quickjs.c"),
            format!("{vendor}/libregexp.c"),
            format!("{vendor}/libunicode.c"),
            format!("{vendor}/dtoa.c"),
            "src/engine/quickjs_compat.c".into(),
        ])
        .warnings(false)
        .compile("musicapp_quickjs");
    println!("cargo:rerun-if-changed=src/engine/quickjs_compat.c");
    println!("cargo:rerun-if-changed=src/engine/quickjs_compat.h");
}
