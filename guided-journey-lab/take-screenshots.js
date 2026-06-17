import { spawn } from 'child_process';
import { chromium } from 'playwright';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DOCS_DIR = path.resolve(__dirname, '../docs');

if (!fs.existsSync(DOCS_DIR)) {
  fs.mkdirSync(DOCS_DIR, { recursive: true });
}

// 1. Start dev server in demo mode
console.log('Starting dev server on port 8091 with VITE_DEMO_MODE=true...');
const devServerEnv = {
  ...process.env,
  VITE_DEMO_MODE: 'true',
};

const devServer = spawn('npx', ['vite', '--port', '8091'], {
  cwd: __dirname,
  env: devServerEnv,
  shell: true,
});

devServer.stdout.on('data', (data) => {
  console.log(`[Vite stdout]: ${data}`);
});

devServer.stderr.on('data', (data) => {
  console.error(`[Vite stderr]: ${data}`);
});

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function isPortOpen(port) {
  try {
    const res = await fetch(`http://127.0.0.1:${port}/`);
    return res.status === 200 || res.status === 404;
  } catch (err) {
    return false;
  }
}

// Wait for dev server to start
console.log('Waiting for dev server to start...');
let started = false;
for (let i = 0; i < 30; i++) {
  if (await isPortOpen(8091)) {
    started = true;
    break;
  }
  await sleep(1000);
}

if (!started) {
  console.error('Failed to start dev server');
  devServer.kill();
  process.exit(1);
}
console.log('Dev server is running!');

// Run browser automation
try {
  const browser = await chromium.launch({ headless: true });
  
  const capture = async (context, urlPath, filename, width = 1280, height = 800) => {
    const page = await context.newPage();
    await page.setViewportSize({ width, height });
    const fullUrl = `http://127.0.0.1:8091${urlPath}`;
    console.log(`Navigating to ${fullUrl} (Viewport: ${width}x${height})...`);
    await page.goto(fullUrl, { waitUntil: 'networkidle' });
    await sleep(2500); // Wait for transitions, client hydration, and images
    const outputPath = path.join(DOCS_DIR, filename);
    await page.screenshot({ path: outputPath });
    console.log(`Saved screenshot: ${outputPath}`);
    await page.close();
  };

  const login = async (context, email, password) => {
    const page = await context.newPage();
    await page.setViewportSize({ width: 1280, height: 800 });
    const fullUrl = 'http://127.0.0.1:8091/login';
    console.log(`Logging in as ${email}...`);
    await page.goto(fullUrl, { waitUntil: 'networkidle' });
    await page.fill('#email', email);
    await page.fill('#password', password);
    await page.click('button[type="submit"]');
    
    // Wait for the URL to change to dashboard, teach, groups, or admin
    await page.waitForURL(
      (url) =>
        url.pathname.includes('/dashboard') ||
        url.pathname.includes('/teach') ||
        url.pathname.includes('/groups') ||
        url.pathname.includes('/admin'),
      { timeout: 10000 }
    );
    await sleep(2000);
    console.log(`Successfully logged in and redirected for ${email}`);
    await page.close();
  };

  const logout = async (context) => {
    const page = await context.newPage();
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.goto('http://127.0.0.1:8091/dashboard', { waitUntil: 'networkidle' });
    await sleep(1000);
    
    await page.evaluate(() => {
      const key = "edulife.website.demo.store.v2";
      const raw = localStorage.getItem(key);
      if (raw) {
        const store = JSON.parse(raw);
        store.session = null;
        localStorage.setItem(key, JSON.stringify(store));
      }
    });
    
    await page.goto('http://127.0.0.1:8091/login', { waitUntil: 'networkidle' });
    console.log('Cleared demo session and returned to login.');
    await page.close();
  };

  // --- 1. Capture Public Pages ---
  let context = await browser.newContext();
  
  // Landing
  await capture(context, '/', 'landing-desktop.png');
  await capture(context, '/', 'landing-mobile.png', 375, 812);

  // Auth pages (Midnight Minimalist theme check)
  await capture(context, '/login', 'auth-login-desktop.png');
  await capture(context, '/register', 'auth-register-role-desktop.png');
  await capture(context, '/register', 'auth-register-role-mobile.png', 375, 812);
  
  // Register step 2 (create credentials step)
  const regPage = await context.newPage();
  await regPage.setViewportSize({ width: 1280, height: 800 });
  await regPage.goto('http://127.0.0.1:8091/register', { waitUntil: 'networkidle' });
  await regPage.click('button:has-text("Continue")');
  await sleep(1000);
  await regPage.screenshot({ path: path.join(DOCS_DIR, 'auth-register-create-account-desktop.png') });
  await regPage.close();

  // Forgot password
  await capture(context, '/forgot-password', 'auth-forgot-password-desktop.png');

  // Public certificate verification
  await capture(context, '/certificates/verify/demo-certificate-french-ui', 'public-certificate-verify-desktop.png');

  await context.close();

  // --- 2. Capture Learner (Student) Portal Pages ---
  context = await browser.newContext();
  await login(context, 'student@edulife.app', 'password');

  await capture(context, '/dashboard', 'learner-dashboard-desktop.png');
  await capture(context, '/dashboard', 'learner-dashboard-mobile.png', 375, 812);

  await capture(context, '/courses', 'course-catalog-desktop.png');
  await capture(context, '/courses/course-darija-web', 'course-details-desktop.png');
  await capture(context, '/learn/course-darija-web/lesson-darija-1', 'lesson-study-desktop.png');
  await capture(context, '/courses/course-darija-web/exam', 'mcq-exam-desktop.png');

  await capture(context, '/planner', 'study-planner-desktop.png');
  await capture(context, '/planner', 'study-planner-mobile.png', 375, 812);

  await capture(context, '/level', 'gamification-level-desktop.png');
  await capture(context, '/level', 'gamification-level-mobile.png', 375, 812);

  await capture(context, '/advisor', 'career-advisor-desktop.png');
  await capture(context, '/advisor', 'career-advisor-mobile.png', 375, 812);

  await capture(context, '/certificates', 'certificates-desktop.png');
  await capture(context, '/certificates/certificate-french-ui', 'certificate-detail-desktop.png');
  
  await capture(context, '/profile', 'profile-desktop.png');

  await logout(context);
  await context.close();

  // --- 3. Capture Teacher Portal Pages ---
  context = await browser.newContext();
  await login(context, 'teacher@edulife.test', 'password');
  await capture(context, '/teach', 'teacher-dashboard-desktop.png');
  await capture(context, '/teach/course-darija-web', 'course-cms-desktop.png');
  await logout(context);
  await context.close();

  // --- 4. Capture Group Admin Portal Pages ---
  context = await browser.newContext();
  await login(context, 'groupadmin@edulife.test', 'password');
  await capture(context, '/groups', 'group-admin-dashboard-desktop.png');
  await logout(context);
  await context.close();

  // --- 5. Capture Platform Admin Portal Pages ---
  context = await browser.newContext();
  await login(context, 'admin@edulife.test', 'password');
  await capture(context, '/admin/dashboard', 'admin-dashboard-desktop.png');
  await capture(context, '/admin/analytics', 'admin-analytics-desktop.png');
  await logout(context);
  await context.close();

  await browser.close();
  console.log('All website screenshots generated successfully!');
} catch (error) {
  console.error('Error during browser automation:', error);
} finally {
  console.log('Stopping dev server...');
  devServer.kill();
}
