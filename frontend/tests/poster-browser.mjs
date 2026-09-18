// Run with a built frontend. Requires Playwright and a Chromium executable; all API data
// and images below are local fixtures. No live services are written to or queried.
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_PACKAGE || 'playwright');
const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const output = resolve(root, 'build/poster-tests'); await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true, ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}) });
const context = await browser.newContext({ viewport: { width: 1440, height: 1100 }, colorScheme: 'dark', acceptDownloads: true });
const page = await context.newPage();
const errors = []; page.on('pageerror', error => errors.push(String(error)));
const logo = Buffer.from(await page.evaluate(() => {
  const c = document.createElement('canvas'); c.width = 1000; c.height = 250;
  const x = c.getContext('2d'); x.fillStyle = '#fff'; x.font = 'bold 110px Arial'; x.textAlign = 'center'; x.fillText('NORTHBOUND', 500, 150);
  x.fillRect(45, 190, 910, 6); return c.toDataURL('image/png').split(',')[1];
}), 'base64');
const opaqueLogo = Buffer.from(await page.evaluate(async (encoded) => {
  const image = new Image(); image.src = 'data:image/png;base64,' + encoded; await image.decode();
  const canvas = document.createElement('canvas'); canvas.width = image.width; canvas.height = image.height;
  const ctx = canvas.getContext('2d'); ctx.fillStyle = '#333333'; ctx.fillRect(0,0,canvas.width,canvas.height); ctx.drawImage(image,0,0);
  return canvas.toDataURL('image/png').split(',')[1];
}, logo.toString('base64')), 'base64');
const requests = [];
const fixture = id => ({ id, title: id === 43 ? null : 'Rails on Fire', date: '2026-10-17', startTime: id === 43 ? null : '20:00:00', status: 'PUBLISHED', createdBy: 4, description: 'Fixture concert', ticketUrl: null, titleImageUrl: null, bandImageDisplay: 'LOGO', eventSeries: null,
  location: { id: 9, name: 'Stellwerk Hamburg', city: 'Hamburg', status: 'PUBLISHED', linkable: true },
  bands: [{ id: 1, name: 'Northbound', genres: ['Heavy Rock', 'Alternative'], logoUrl: id === 43 ? null : id === 44 ? 'http://127.0.0.1:19081/uploads/opaque.png' : 'http://127.0.0.1:19081/uploads/test.png', status: 'PUBLISHED', linkable: true }, { id: 2, name: 'Over the River', genres: ['Groove Metal'], logoUrl: null, status: 'PUBLISHED', linkable: true }, { id: 3, name: 'Desert Sun', genres: ['Stoner Rock'], logoUrl: null, status: 'PUBLISHED', linkable: true }] });
const backend = createServer((req, res) => {
  requests.push(req.method + ' ' + req.url);
  if (req.url === '/uploads/opaque.png') { res.writeHead(200, { 'Content-Type': 'image/png' }); res.end(opaqueLogo); return; }
  if (req.url === '/uploads/test.png') { res.writeHead(200, { 'Content-Type': 'image/png' }); res.end(logo); return; }
  res.setHeader('Content-Type', 'application/json');
  if (req.url === '/api/me') { res.end(JSON.stringify({ id: req.headers.authorization === 'Bearer viewer' ? 999 : 4, email: 'test@example.org', username: 'Poster Test', platformAdmin: false, mustChangePassword: false, managedEntities: [], followedBands: [], savedEvents: [], savedActs: [], preferredGenres: ['Rock'] })); return; }
  if (/^\/api\/events\/(42|43|44)$/.test(req.url)) { res.end(JSON.stringify(fixture(Number(req.url.split('/').at(-1))))); return; }
  if (req.url === '/api/locations/9') { res.end(JSON.stringify({ id: 9, name: 'Stellwerk Hamburg', city: 'Hamburg', upcomingEvents: [] })); return; }
  res.writeHead(404); res.end('{}');
});
await new Promise(resolve => backend.listen(19081, '127.0.0.1', resolve));
let log = '';
const server = spawn(process.execPath, ['node_modules/next/dist/bin/next', 'start', '-p', '3218', '-H', '127.0.0.1'], { cwd: root, windowsHide: true, env: { ...process.env, API_BASE_URL: 'http://127.0.0.1:19081', NEXT_TELEMETRY_DISABLED: '1' }, stdio: ['ignore','pipe','pipe'] });
server.stdout.on('data', chunk => { log += chunk; }); server.stderr.on('data', chunk => { log += chunk; });
const base = 'http://127.0.0.1:3218';
try {
  for (let i = 0; i < 100; i++) { try { await fetch(base + '/login'); break; } catch { await new Promise(r => setTimeout(r, 100)); } }
  await page.goto(base + '/konzerte/42/plakat'); await page.waitForURL('**/login');
  await context.addCookies([{ name: 'giglister_token', value: 'viewer', url: base }]);
  await page.goto(base + '/konzerte/42/plakat'); await page.waitForURL('**/konzerte/42');
  assert.equal((await context.request.get(base + '/konzerte/42/plakat/bild?band=1')).status(), 403);
  await context.addCookies([{ name: 'giglister_token', value: 'tester', url: base }]);
  await page.goto(base + '/konzerte/42'); await page.getByRole('link', { name: 'Plakat gestalten' }).click();
  await page.getByRole('button', { name: 'Weiter zum Designer →' }).waitFor();
  await page.waitForFunction(() => !document.querySelector('aside button:last-of-type')?.disabled);
  const variants = new Set();
  for (const pattern of ['Verlauf', 'Strahlen', 'Streifen', 'Punkte', 'Körnung']) {
    await page.getByLabel('Muster', { exact: true }).selectOption(pattern);
    await page.evaluate(() => new Promise(requestAnimationFrame));
    variants.add(await page.locator('canvas').evaluate(canvas => canvas.toDataURL()));
  }
  assert.equal(variants.size, 5);
  await page.getByLabel('Muster', { exact: true }).selectOption('Strahlen');
  await page.getByRole('button', { name: 'Weiter zum Designer →' }).click();
  await page.getByLabel('Ebene auswählen').selectOption('band-1');
  await page.getByText('Logo: transparente Pixel erkannt.').waitFor(); assert.equal(await page.getByLabel('Rahmen um dieses Logo').count(), 0);
  const preview = page.getByLabel('Plakatvorschau'); await preview.focus(); await preview.press('ArrowRight');
  await page.getByLabel('Größe', { exact: true }).focus(); await page.getByLabel('Größe', { exact: true }).press('ArrowRight');
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  let draft = await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-42')));
  assert.equal(draft.layers[1].x, 510); assert.ok(draft.layers[1].scale > 1); assert.equal(draft.layers[1].genre, 'Heavy Rock');
  const rect = await preview.boundingBox();
  const band = draft.layers.find(layer => layer.id === 'band-2');
  const x = rect.x + band.x / 1000 * rect.width, y = rect.y + band.y / 1414 * rect.height;
  await page.mouse.move(x, y); await page.mouse.down(); await page.mouse.move(x + 24, y - 12, { steps: 5 }); await page.mouse.up();
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  assert.ok(await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-42')).layers.find(layer => layer.id === 'band-2').x > 275));
  await page.getByRole('button', { name: 'Rückgängig', exact: true }).click();
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-42')).layers.find(layer => layer.id === 'band-2').x), 275);
  await page.reload(); await page.getByRole('button', { name: 'Entwurf laden', exact: true }).click();
  await page.getByRole('button', { name: 'Plakat als PNG herunterladen' }).waitFor();
  await page.getByLabel('Ebene auswählen').selectOption('footer');
  await page.screenshot({ path: resolve(output, 'poster-desktop.png'), fullPage: true });
  const downloadPromise = page.waitForEvent('download'); await page.getByRole('button', { name: 'Plakat als PNG herunterladen' }).click();
  const download = await downloadPromise; await download.saveAs(resolve(output, 'poster-export.png'));
  const png = await readFile(resolve(output, 'poster-export.png')); assert.equal(png.readUInt32BE(16), 2480); assert.equal(png.readUInt32BE(20), 3508);
  await page.getByRole('button', { name: '← Hintergrund' }).click();
  await page.getByLabel('Hintergrundbild auswählen').setInputFiles({ name: 'photo.png', mimeType: 'image/png', buffer: logo });
  await page.getByRole('button', { name: 'Stattdessen Muster verwenden' }).waitFor();
  await page.getByRole('button', { name: 'Weiter zum Designer →' }).click();
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  draft = await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-42'))); assert.ok(draft.background.image.startsWith('data:image/png;base64,'));
  await page.getByRole('button', { name: '← Hintergrund' }).click();
  await page.getByLabel('Hintergrundbild auswählen').setInputFiles({ name: 'invalid.txt', mimeType: 'text/plain', buffer: Buffer.from('invalid') });
  await page.getByText('Bitte ein JPG-, PNG- oder WebP-Bild auswählen.').waitFor();
  await page.goto(base + '/konzerte/43/plakat'); await page.getByRole('button', { name: 'Weiter zum Designer →' }).click();
  assert.equal(await page.getByLabel('Ebene auswählen').locator('option[value="title"]').count(), 0);
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  const noTitle = await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-43'))); assert.equal(noTitle.layers.at(-1).genre, '17.10.2026');
  await page.setViewportSize({ width: 390, height: 844 }); await page.screenshot({ path: resolve(output, 'poster-mobile.png'), fullPage: true });
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  const touch = await context.newCDPSession(page);
  const mobileRect = await preview.boundingBox();
  const center = { x: mobileRect.x + mobileRect.width / 2, y: mobileRect.y + (357.5 / 1414) * mobileRect.height };
  await touch.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: center.x - 20, y: center.y, id: 1 }, { x: center.x + 20, y: center.y, id: 2 }] });
  await touch.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: center.x - 32, y: center.y - 10, id: 1 }, { x: center.x + 32, y: center.y + 10, id: 2 }] });
  await touch.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  const pinched = await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-43')).layers.find(layer => layer.id === 'band-1'));
  assert.ok(pinched.scale > 1.4); assert.ok(pinched.rotation > 10);
  assert.equal((await context.request.get(base + '/konzerte/42/plakat/bild?band=999')).status(), 404);
  await page.setViewportSize({ width: 1440, height: 1100 });
  await page.goto(base + '/konzerte/44/plakat'); await page.getByRole('button', { name: 'Weiter zum Designer →' }).click();
  await page.getByLabel('Ebene auswählen').selectOption('band-1');
  await page.getByText('Logo: nicht transparent. Du kannst einen Rahmen ergänzen.').waitFor();
  await page.getByLabel('Rahmen um dieses Logo').check();
  await page.getByLabel('Rahmenstärke', { exact: true }).focus(); await page.getByLabel('Rahmenstärke', { exact: true }).press('ArrowRight');
  await page.getByRole('button', { name: 'Entwurf speichern', exact: true }).click();
  const framed = await page.evaluate(() => JSON.parse(localStorage.getItem('giglister-poster-v1-44')).layers.find(layer => layer.id === 'band-1'));
  assert.equal(framed.logoFrame, true); assert.equal(framed.logoFrameWidth, 7);
  await page.reload(); await page.getByRole('button', { name: 'Entwurf laden', exact: true }).click();
  await page.getByLabel('Ebene auswählen').selectOption('band-1'); await page.getByLabel('Rahmenstärke', { exact: true }).waitFor();
  assert.equal(await page.getByLabel('Rahmen um dieses Logo').isChecked(), true);
  await page.screenshot({ path: resolve(output, 'poster-logo-frame.png'), fullPage: true });
  const framedDownload = page.waitForEvent('download'); await page.getByRole('button', { name: 'Plakat als PNG herunterladen' }).click();
  await (await framedDownload).saveAs(resolve(output, 'poster-framed-export.png'));
  await page.getByLabel('Ebene auswählen').selectOption('band-2'); assert.equal(await page.getByLabel('Rahmen um dieses Logo').count(), 0);
  assert.ok(requests.every(request => request.startsWith('GET '))); assert.deepEqual(errors, []);
  console.log('PASS: permissions, event entry, logo proxy, patterns, keyboard transform, resize, draft restore, PNG export, image import, invalid file, title/time fallback, mobile layout; only local GET API calls.');
} finally {
  await browser.close(); server.kill(); backend.close(); await writeFile(resolve(output, 'server.log'), log);
}
