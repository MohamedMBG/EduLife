import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const docsDir = scriptDir;
const repoRoot = path.resolve(docsDir, "..");
const frontendDir = path.join(repoRoot, "guided-journey-lab");
const backendDir = path.join(repoRoot, "backend");
const outputDir = path.join(docsDir, "2026-06-17-live-project-inspection-assets");
const inventoryJsonPath = path.join(docsDir, "2026-06-17-live-screenshot-inventory.json");
const inventoryMdPath = path.join(docsDir, "2026-06-17-live-screenshot-inventory.md");

const FRONTEND_ORIGIN = "http://localhost:8080";
const DESKTOP_VIEWPORT = { width: 1440, height: 1024 };

// These routes were selected from the live application inventory and current seeded data so the
// capture run stays read-only while still covering each role's real experience.
const CAPTURE_PLAN = {
  public: [
    { key: "landing", path: "/", title: "Page d'accueil" },
    { key: "login", path: "/login", title: "Connexion" },
    { key: "register", path: "/register", title: "Inscription" },
    { key: "forgot-password", path: "/forgot-password", title: "Mot de passe oublie" },
    {
      key: "certificate-verify",
      path: "/certificates/verify/100ea86fc7a525cc04d8a0da3ea7bdcbb345ee63d31524b45d5c46a032f56a25",
      title: "Verification publique de certificat",
    },
  ],
  student: [
    { key: "dashboard", path: "/dashboard", title: "Tableau de bord etudiant" },
    { key: "explore", path: "/explore", title: "Catalogue de cours" },
    {
      key: "course-detail",
      path: "/courses/11111111-1111-1111-1111-111111111111",
      title: "Detail d'un cours",
    },
    {
      key: "lesson",
      path: "/learn/11111111-1111-1111-1111-111111111111/11111111-aaaa-0000-0000-111111111111",
      title: "Lecture d'une lecon",
    },
    { key: "analytics", path: "/analytics", title: "Analytics etudiant" },
    { key: "advisor", path: "/advisor", title: "Career Advisor" },
    { key: "planner", path: "/planner", title: "Study Planner" },
    { key: "level", path: "/level", title: "Gamification" },
    { key: "certificates", path: "/certificates", title: "Liste des certificats" },
    {
      key: "certificate-detail",
      path: "/certificates/2baceee0-61d7-4ac8-8669-5dab60cc9274",
      title: "Detail d'un certificat",
    },
    { key: "profile", path: "/profile", title: "Profil etudiant" },
  ],
  student_exam: [
    {
      key: "exam",
      path: "/courses/55555555-5555-5555-5555-555555555555/exam",
      title: "Examen MCQ en direct",
    },
  ],
  teacher: [
    { key: "dashboard", path: "/teach", title: "Portail enseignant" },
    {
      key: "course-management",
      path: "/teach/556ff814-51a3-488b-93ce-b86692e819eb",
      title: "Gestion d'un cours enseignant",
    },
    {
      key: "exam-builder",
      path: "/teach/556ff814-51a3-488b-93ce-b86692e819eb/exam",
      title: "Gestionnaire d'examen enseignant",
    },
    { key: "analytics", path: "/analytics", title: "Suivi des cohortes enseignant" },
    { key: "profile", path: "/profile", title: "Profil enseignant" },
  ],
  group_admin: [
    { key: "dashboard", path: "/groups", title: "Portail group admin" },
    {
      key: "group-detail",
      path: "/groups/e4902052-b02f-4993-9282-49870eabab0c",
      title: "Detail d'un groupe",
    },
    { key: "approvals", path: "/approvals", title: "Approbation des cours" },
    { key: "analytics", path: "/analytics", title: "Analytics group admin" },
    { key: "profile", path: "/profile", title: "Profil group admin" },
  ],
  admin: [
    { key: "dashboard", path: "/admin/dashboard", title: "Dashboard administrateur" },
    {
      key: "teacher-requests",
      path: "/admin/teacher-requests",
      title: "Demandes enseignants",
    },
    { key: "analytics", path: "/admin/analytics", title: "Analytics administrateur" },
    { key: "profile", path: "/profile", title: "Profil administrateur" },
  ],
};

const ROLE_SESSIONS = {
  student: {
    label: "Etudiant",
    uid: "cMbHQAYQ9bbwnc2QqBRTAMar8UE2",
    captureGroup: "student",
  },
  student_exam: {
    label: "Etudiant",
    uid: "d14kAUgJj9OoAxFAqKgAcefbg8n2",
    captureGroup: "student_exam",
  },
  teacher: {
    label: "Enseignant",
    uid: "H64vfuU7lfRANJfHFqUmCCxGAoz2",
    captureGroup: "teacher",
  },
  group_admin: {
    label: "Group Admin",
    uid: "m512sVctrhfgKnsBJapVaMxGflw1",
    captureGroup: "group_admin",
  },
  admin: {
    label: "Administrateur",
    uid: "sdPxlkyuEKS0ZGyFyMoHv0C7xeQ2",
    captureGroup: "admin",
  },
};

function parseEnvFile(content) {
  const env = {};
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) {
      continue;
    }

    const separatorIndex = line.indexOf("=");
    if (separatorIndex < 0) {
      continue;
    }

    const key = line.slice(0, separatorIndex).trim();
    let value = line.slice(separatorIndex + 1).trim();

    if (
      (value.startsWith("\"") && value.endsWith("\"")) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    env[key] = value;
  }

  return env;
}

function base64UrlEncode(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function signJwt(payload, privateKey) {
  const header = { alg: "RS256", typ: "JWT" };
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const unsignedToken = `${encodedHeader}.${encodedPayload}`;
  const signature = crypto.createSign("RSA-SHA256").update(unsignedToken).sign(privateKey);
  return `${unsignedToken}.${base64UrlEncode(signature)}`;
}

async function getGoogleAccessToken(serviceAccount) {
  const now = Math.floor(Date.now() / 1000);
  const assertion = signJwt(
    {
      iss: serviceAccount.client_email,
      sub: serviceAccount.client_email,
      aud: "https://oauth2.googleapis.com/token",
      scope: "https://www.googleapis.com/auth/identitytoolkit",
      iat: now,
      exp: now + 3600,
    },
    serviceAccount.private_key,
  );

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!response.ok) {
    throw new Error(`OAuth token request failed with status ${response.status}.`);
  }

  const payload = await response.json();
  return payload.access_token;
}

function createCustomToken(serviceAccount, uid) {
  const now = Math.floor(Date.now() / 1000);
  return signJwt(
    {
      iss: serviceAccount.client_email,
      sub: serviceAccount.client_email,
      aud: "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit",
      iat: now,
      exp: now + 3600,
      uid,
    },
    serviceAccount.private_key,
  );
}

async function signInWithCustomToken(apiKey, customToken) {
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        token: customToken,
        returnSecureToken: true,
      }),
    },
  );

  if (!response.ok) {
    throw new Error(`Firebase custom token sign-in failed with status ${response.status}.`);
  }

  return response.json();
}

async function ensureDirectory(dirPath) {
  await fs.mkdir(dirPath, { recursive: true });
}

async function loadConfiguration() {
  const frontendEnv = parseEnvFile(await fs.readFile(path.join(frontendDir, ".env"), "utf8"));
  const backendEnv = parseEnvFile(await fs.readFile(path.join(backendDir, ".env"), "utf8"));
  const serviceAccountPath =
    backendEnv.FIREBASE_ADMIN_CREDENTIALS_PATH || process.env.FIREBASE_ADMIN_CREDENTIALS_PATH;

  if (!serviceAccountPath) {
    throw new Error("FIREBASE_ADMIN_CREDENTIALS_PATH is missing in backend/.env.");
  }

  const serviceAccount = JSON.parse(await fs.readFile(serviceAccountPath, "utf8"));

  return {
    frontendEnv,
    serviceAccount,
  };
}

async function settlePage(page) {
  await page.waitForLoadState("domcontentloaded");
  await page.waitForTimeout(1500);
  await page.waitForFunction(() => document.readyState === "complete");
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => undefined);
  await page.waitForTimeout(500);
}

async function waitForLiveContent(page) {
  const loadingMarkers = [
    "Loading your EduLife session",
    "Loading admin session",
    "Loading course",
    "Loading certificates",
    "Loading analytics",
    "Loading admin metrics",
    "Loading profile",
    "Loading your groups",
    "Loading course content",
  ];

  try {
    await page.waitForFunction(
      (markers) => {
        const text = document.body.innerText || "";
        return markers.every((marker) => !text.includes(marker));
      },
      loadingMarkers,
      { timeout: 25000 },
    );
    await page.waitForTimeout(800);
    return { loadedSuccessfully: true, note: "" };
  } catch {
    const bodyText = await page.locator("body").innerText().catch(() => "");
    if (bodyText.includes("Cannot reach the server")) {
      return {
        loadedSuccessfully: false,
        note: "Capture partielle : le frontend affiche une erreur de connexion serveur.",
      };
    }

    return {
      loadedSuccessfully: false,
      note: "Capture partielle : l'ecran est reste en etat de chargement apres 25 secondes.",
    };
  }
}

async function browserSignIn(page, firebaseConfig, customToken) {
  await page.goto(`${FRONTEND_ORIGIN}/login`, { waitUntil: "domcontentloaded" });

  await page.evaluate(
    async ({ config, token }) => {
      const appModule = await import("https://www.gstatic.com/firebasejs/12.14.0/firebase-app.js");
      const authModule = await import(
        "https://www.gstatic.com/firebasejs/12.14.0/firebase-auth.js"
      );

      const app = appModule.getApps().length
        ? appModule.getApp()
        : appModule.initializeApp(config);
      const auth = authModule.getAuth(app);

      await authModule.signInWithCustomToken(auth, token);
    },
    { config: firebaseConfig, token: customToken },
  );

  await page.reload({ waitUntil: "domcontentloaded" });
  await settlePage(page);
}

async function capturePublicPages(browser, inventory) {
  const context = await browser.newContext({
    viewport: DESKTOP_VIEWPORT,
    deviceScaleFactor: 1,
  });
  const page = await context.newPage();
  const publicDir = path.join(outputDir, "public");

  for (const screen of CAPTURE_PLAN.public) {
    const targetPath = path.join(publicDir, `${screen.key}.png`);
    const route = `${FRONTEND_ORIGIN}${screen.path}`;

    try {
      await page.goto(route, { waitUntil: "domcontentloaded" });
      await settlePage(page);
      const result = await waitForLiveContent(page);
      await page.screenshot({ path: targetPath });

      inventory.push({
        role: "Public",
        screen: screen.title,
        route: screen.path,
        file: path.relative(docsDir, targetPath).replace(/\\/g, "/"),
        status: result.loadedSuccessfully ? "captured" : "partial",
        loadedSuccessfully: result.loadedSuccessfully,
        note: result.note,
      });
    } catch (error) {
      inventory.push({
        role: "Public",
        screen: screen.title,
        route: screen.path,
        file: "",
        status: "failed",
        loadedSuccessfully: false,
        note: error instanceof Error ? error.message : String(error),
      });
    }
  }

  await context.close();
}

async function captureRolePages(browser, firebaseConfig, inventory) {
  for (const session of Object.values(ROLE_SESSIONS)) {
    const context = await browser.newContext({
      viewport: DESKTOP_VIEWPORT,
      deviceScaleFactor: 1,
    });
    const page = await context.newPage();
    const roleDir = path.join(outputDir, session.captureGroup);
    const customToken = createCustomToken(firebaseConfig.serviceAccount, session.uid);

    try {
      await browserSignIn(page, firebaseConfig.clientConfig, customToken);
    } catch (error) {
      for (const screen of CAPTURE_PLAN[session.captureGroup]) {
        inventory.push({
          role: session.label,
          screen: screen.title,
          route: screen.path,
          file: "",
          status: "failed",
          loadedSuccessfully: false,
          note: `Authentication failed: ${
            error instanceof Error ? error.message : String(error)
          }`,
        });
      }
      await context.close();
      continue;
    }

    for (const screen of CAPTURE_PLAN[session.captureGroup]) {
      const targetPath = path.join(roleDir, `${screen.key}.png`);

      try {
        await page.goto(`${FRONTEND_ORIGIN}${screen.path}`, { waitUntil: "domcontentloaded" });
        await settlePage(page);
        const result = await waitForLiveContent(page);
        await page.screenshot({ path: targetPath });

        inventory.push({
          role: session.label,
          screen: screen.title,
          route: screen.path,
          file: path.relative(docsDir, targetPath).replace(/\\/g, "/"),
          status: result.loadedSuccessfully ? "captured" : "partial",
          loadedSuccessfully: result.loadedSuccessfully,
          note: result.note,
        });
      } catch (error) {
        inventory.push({
          role: session.label,
          screen: screen.title,
          route: screen.path,
          file: "",
          status: "failed",
          loadedSuccessfully: false,
          note: error instanceof Error ? error.message : String(error),
        });
      }
    }

    await context.close();
  }
}

async function writeInventory(inventory) {
  await fs.writeFile(inventoryJsonPath, JSON.stringify(inventory, null, 2));

  const markdown = [
    "# Inventaire des captures live",
    "",
    `Date: ${new Date().toISOString()}`,
    "",
    "| Role | Ecran | Route | Fichier | Statut | Chargement | Note |",
    "| --- | --- | --- | --- | --- | --- | --- |",
    ...inventory.map((item) => {
      const note = item.note ? item.note.replace(/\|/g, "\\|") : "";
      return `| ${item.role} | ${item.screen} | \`${item.route}\` | ${
        item.file ? `\`${item.file}\`` : "-"
      } | ${item.status} | ${item.loadedSuccessfully ? "OK" : "KO"} | ${note || "-"} |`;
    }),
    "",
  ].join("\n");

  await fs.writeFile(inventoryMdPath, markdown);
}

async function main() {
  const { frontendEnv, serviceAccount } = await loadConfiguration();

  if (!frontendEnv.VITE_FIREBASE_API_KEY) {
    throw new Error("VITE_FIREBASE_API_KEY is missing in guided-journey-lab/.env.");
  }

  const firebaseConfig = {
    serviceAccount,
    clientConfig: {
      apiKey: frontendEnv.VITE_FIREBASE_API_KEY,
      authDomain: frontendEnv.VITE_FIREBASE_AUTH_DOMAIN,
      projectId: frontendEnv.VITE_FIREBASE_PROJECT_ID,
      storageBucket: frontendEnv.VITE_FIREBASE_STORAGE_BUCKET,
      messagingSenderId: frontendEnv.VITE_FIREBASE_MESSAGING_SENDER_ID,
      appId: frontendEnv.VITE_FIREBASE_APP_ID,
    },
  };

  const inventory = [];

  // Clearing the output directory each run keeps the report reproducible and prevents stale images
  // from being mistaken for current localhost captures.
  await fs.rm(outputDir, { recursive: true, force: true });
  await ensureDirectory(outputDir);
  await Promise.all(
    ["public", "student", "student_exam", "teacher", "group_admin", "admin"].map((dirName) =>
      ensureDirectory(path.join(outputDir, dirName)),
    ),
  );

  const browser = await chromium.launch({ headless: true });

  try {
    await capturePublicPages(browser, inventory);
    await captureRolePages(browser, firebaseConfig, inventory);
  } finally {
    await browser.close();
  }

  await writeInventory(inventory);

  const summary = inventory.reduce(
    (accumulator, item) => {
      if (item.status === "captured") {
        accumulator.captured += 1;
      } else {
        accumulator.failed += 1;
      }
      return accumulator;
    },
    { captured: 0, failed: 0 },
  );

  console.log(
    JSON.stringify(
      {
        outputDir,
        inventoryJsonPath,
        inventoryMdPath,
        summary,
      },
      null,
      2,
    ),
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
