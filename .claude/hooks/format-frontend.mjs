import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";
import path from "node:path";

const input = JSON.parse(readFileSync(0, "utf8"));
const file = input.tool_response?.filePath ?? input.tool_input?.file_path;
const frontend = path.resolve(process.env.CLAUDE_PROJECT_DIR ?? ".", "frontend");
const relative = file ? path.relative(frontend, path.resolve(file)) : "";

if (!relative || relative.startsWith("..") || path.isAbsolute(relative)) process.exit(0);

// --no-install: do nothing until prettier is a frontend devDependency.
spawnSync("npx", ["--no-install", "prettier", "--write", "--ignore-unknown", relative], {
  cwd: frontend,
  stdio: "ignore",
  shell: process.platform === "win32",
});
