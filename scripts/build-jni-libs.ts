import { execSync } from "node:child_process";
import { ROOT, RUST_LIBS_ROOTS, TARGETS } from "./base";
import path from "node:path";

console.log("Build app backend in debug mode");
execSync(`cargo build -p app-backend`, {
  stdio: "inherit",
  cwd: RUST_LIBS_ROOTS,
});

for (const buildTarget of TARGETS) {
  console.log(`Generate jniLibs of ${buildTarget}`);
  execSync(
    `cargo ndk --no-strip --platform 34 --target ${buildTarget} -o ${path.resolve(ROOT, "androidApp/src/main/jniLibs")} build --release --lib`,
    {
      stdio: "inherit",
      cwd: RUST_LIBS_ROOTS,
      env: {
        ...process.env,
        RUST_BACKTRACE: "1",
      },
    },
  );
}

console.log("Generate kotlin bindings");
execSync(
  `cargo run -p uniffi-bindgen generate --library ${path.resolve(RUST_LIBS_ROOTS, "./target/debug/libapp_backend.so")} --language kotlin --out-dir ${path.resolve(ROOT, "shared/src/commonMain/kotlin/")}`,
  {
    stdio: "inherit",
    cwd: RUST_LIBS_ROOTS,
    env: {
      ...process.env,
      RUST_BACKTRACE: "1",
    },
  },
);

console.log("Build desktop native library");
execSync(`cargo build --release --lib`, {
  stdio: "inherit",
  cwd: RUST_LIBS_ROOTS,
});
