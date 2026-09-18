import test from 'node:test';
import assert from 'node:assert/strict';
import { initialPoster, restorePoster, hitLayer, posterDate } from '../src/lib/poster.ts';
const event = (count, title = 'Rails on Fire') => ({ id: 7, title, date: '2026-09-17', startTime: '20:00:00', location: { name: 'Stellwerk Hamburg' }, bands: Array.from({ length: count }, (_, i) => ({ id: i + 1, name: `Band ${i + 1}`, logoUrl: i ? null : '/uploads/logo.png', genres: ['Rock', 'Punk'] })) });
test('optional event name and first genre are taken from concert data', () => {
  const draft = initialPoster(event(3, null));
  assert.equal(draft.layers.some(l => l.kind === 'title'), false);
  assert.equal(draft.layers.filter(l => l.kind === 'band').length, 3);
  assert.equal(draft.layers[0].genre, 'Rock');
  assert.equal(draft.layers[1].text, 'Band 2');
  assert.equal(draft.layers.at(-1).text, 'Stellwerk Hamburg');
  assert.equal(draft.layers.at(-1).genre, '17.09.2026 · 20:00 UHR');
  assert.equal(posterDate('2026-09-17', null), '17.09.2026');
});
test('lineups from one to twenty have nonoverlapping slots clear of title and footer', () => {
  for (let count = 1; count <= 20; count++) {
    const layers = initialPoster(event(count)).layers;
    for (const layer of layers) {
      assert.ok(layer.x - layer.width / 2 >= 0);
      assert.ok(layer.x + layer.width / 2 <= 1000);
      assert.ok(layer.y - layer.height / 2 >= 0);
      assert.ok(layer.y + layer.height / 2 <= 1414);
    }
    layers.forEach((a, i) => layers.slice(i + 1).forEach(b => {
      assert.ok(Math.abs(a.x - b.x) >= (a.width + b.width) / 2 || Math.abs(a.y - b.y) >= (a.height + b.height) / 2, `${count}: ${a.id} overlaps ${b.id}`);
    }));
  }
});
test('saved transforms restore but metadata and URLs always use current concert', () => {
  const current = event(3); const draft = initialPoster(current); draft.layers[1].rotation = 23; draft.layers[1].x = 600;
  draft.layers[1].logoUrl = 'https://untrusted.invalid/file'; draft.layers[1].text = 'wrong';
  const restored = restorePoster(JSON.stringify(draft), current);
  assert.equal(restored.layers[1].x, 600); assert.equal(restored.layers[1].rotation, 23);
  assert.equal(restored.layers[1].logoUrl, '/uploads/logo.png'); assert.equal(restored.layers[1].text, 'Band 1');
});
test('invalid drafts, changed lineups, and foreign event ids are rejected', () => {
  const current = event(2); const draft = initialPoster(current);
  assert.throws(() => restorePoster('{}', current));
  assert.throws(() => restorePoster(JSON.stringify({ ...draft, eventId: 8 }), current));
  assert.throws(() => restorePoster(JSON.stringify(draft), event(3)));
  draft.background.image = 'https://external.invalid/image'; assert.throws(() => restorePoster(JSON.stringify(draft), current));
});
test('selection respects rotated and scaled layer bounds', () => {
  const layer = { ...initialPoster(event(1)).layers[1], x: 100, y: 100, width: 200, height: 40, scale: 2, rotation: 90 };
  assert.equal(hitLayer(layer, 100, 280), true); assert.equal(hitLayer(layer, 280, 100), false);
});
test('logo frames persist and old drafts receive disabled defaults', () => {
  const current = event(2); const draft = initialPoster(current);
  draft.layers[1].logoFrame = true; draft.layers[1].logoFrameColor = '#ee4400'; draft.layers[1].logoFrameWidth = 12;
  const restored = restorePoster(JSON.stringify(draft), current);
  assert.equal(restored.layers[1].logoFrame, true); assert.equal(restored.layers[1].logoFrameWidth, 12); assert.equal(restored.layers[1].logoFrameColor, '#ee4400');
  for (const layer of draft.layers) { delete layer.logoFrame; delete layer.logoFrameWidth; delete layer.logoFrameColor; }
  assert.equal(restorePoster(JSON.stringify(draft), current).layers[1].logoFrame, false);
  draft.layers[1].logoFrameWidth = -5; assert.throws(() => restorePoster(JSON.stringify(draft), current));
});
