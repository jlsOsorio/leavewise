import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";

const input = JSON.parse(readFileSync(0, "utf8"));
const file = input.tool_response?.filePath ?? input.tool_input?.file_path;
const frontend = path.resolve(process.env.CLAUDE_PROJECT_DIR ?? ".", "frontend");
const relative = file ? path.relative(frontend, path.resolve(file)) : "";

if (!relative || relative.startsWith("..") || path.isAbsolute(relative)) process.exit(0);
// Nothing to run before `npm install`
if (!existsSync(path.join(frontend, "node_modules"))) process.exit(0);

const LINTABLE = /\.[cm]?[jt]sx?$/;

const run = (command, args) =>
  spawnSync(command, args, {
    cwd: frontend,
    stdio: "ignore",
    shell: process.platform === "win32",
  });

// ESLint first for fixable rules (import order), then Prettier (frontend/.prettierrc.json) has the last word.
// Prettier runs on the single file: `npm run format` would format the whole project.
if (LINTABLE.test(relative)) run("npm", ["run", "eslint:fix", "--", relative]);
run("npx", ["--no-install", "prettier", "--write", "--ignore-unknown", relative]);
