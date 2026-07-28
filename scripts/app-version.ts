import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import path from "node:path";

export type AppVersion = {
  name: string;
  code: number;
};

const STABLE_TAG = /^v(\d+\.\d+\.\d+)$/;
const PRERELEASE_TAG = /^pre-v(\d+\.\d+\.\d+-beta\.\d+)$/;

function git(root: string, args: string[]): string {
  try {
    return execFileSync("git", args, {
      cwd: root,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
  } catch {
    return "";
  }
}

function readVersionBase(root: string): string {
  const properties = readFileSync(
    path.join(root, "gradle.properties"),
    "utf8",
  );
  const match = properties.match(/^appVersionBase=(\d+\.\d+\.\d+)$/m);
  if (!match) {
    throw new Error("gradle.properties must define appVersionBase as X.Y.Z");
  }
  return match[1];
}

function versionFromTags(tags: string[]): string | undefined {
  for (const pattern of [STABLE_TAG, PRERELEASE_TAG]) {
    const version = tags
      .map((tag) => tag.match(pattern)?.[1])
      .find((candidate) => candidate !== undefined);
    if (version) return version;
  }
  return undefined;
}

function positiveInteger(value: string | undefined): number | undefined {
  if (!value || !/^[1-9]\d*$/.test(value)) return undefined;
  return Number(value);
}

export function resolveAppVersion(
  root: string,
  env: NodeJS.ProcessEnv = process.env,
): AppVersion {
  const commitCount =
    positiveInteger(git(root, ["rev-list", "--count", "HEAD"])) ?? 1;
  const commitSha = git(root, ["rev-parse", "--short=12", "HEAD"]) || "unknown";
  const tags = git(root, ["tag", "--points-at", "HEAD"])
    .split(/\r?\n/)
    .filter(Boolean);
  const name =
    env.APP_VERSION_NAME?.trim() ||
    versionFromTags(tags) ||
    `${readVersionBase(root)}-dev.${commitCount}+${commitSha}`;
  const code = positiveInteger(env.APP_VERSION_CODE) ?? commitCount;

  return { name, code };
}
