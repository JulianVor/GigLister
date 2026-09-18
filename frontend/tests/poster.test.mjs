import test from 'node:test';
import assert from 'node:assert/strict';
import { initialPoster, restorePoster, hitLayer, posterDate, ticketProviderLabel, luminanceTextColor, backgroundGradientLuminance, patterns } from '../src/lib/poster.ts';
const event = (count, title = 'Rails on Fire', ticketUrl = null) => ({ id: 7, title, date: '2026-09-17', startTime: '20:00:00', location: { name: 'Stellwerk Hamburg' }, ticketUrl, bands: Array.from({ length: count }, (_, i) => ({ id: i + 1, name: `Band ${i + 1}`, logoUrl: i ? null : '/uploads/logo.png', genres: ['Rock', 'Punk'] })) });
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
test('ticket provider label is the bare domain, or null when there is none', () => {
  assert.equal(ticketProviderLabel('https://www.tix4gigs.com/event/123'), 'tix4gigs.com');
  assert.equal(ticketProviderLabel('http://hhgigs.com'), 'hhgigs.com');
  assert.equal(ticketProviderLabel('www.eventim.de/foo'), 'eventim.de');
  assert.equal(ticketProviderLabel('  '), null);
  assert.equal(ticketProviderLabel(null), null);
  assert.equal(ticketProviderLabel(undefined), null);
  assert.equal(ticketProviderLabel('not a url'), null);
});
test('the footer moves up and carries a ticket label only when the concert has a ticket link', () => {
  const withTicket = initialPoster(event(2, 'Rails on Fire', 'https://www.tix4gigs.com'));
  const withoutTicket = initialPoster(event(2));
  assert.equal(withTicket.ticketLabel, 'tix4gigs.com');
  assert.equal(withoutTicket.ticketLabel, null);
  const footer = draft => draft.layers.find(l => l.kind === 'footer');
  assert.ok(footer(withTicket).y < footer(withoutTicket).y);
  assert.equal(footer(withoutTicket).y + footer(withoutTicket).height / 2, 1414);
});
test('a restored draft always re-derives the ticket label and corner radius from the current concert', () => {
  const current = event(2, 'Rails on Fire', 'https://www.tix4gigs.com');
  const draft = initialPoster(current); draft.cornerRadius = 30;
  const restored = restorePoster(JSON.stringify(draft), current);
  assert.equal(restored.cornerRadius, 30);
  assert.equal(restored.ticketLabel, 'tix4gigs.com');
  const restoredWithoutTicket = restorePoster(JSON.stringify(draft), event(2, 'Rails on Fire'));
  assert.equal(restoredWithoutTicket.ticketLabel, null);
  delete draft.cornerRadius;
  assert.equal(restorePoster(JSON.stringify(draft), current).cornerRadius, 0);
  draft.cornerRadius = 500; assert.throws(() => restorePoster(JSON.stringify(draft), current));
});
test('suggested text color reads black or white off the gradient/dim combination', () => {
  assert.equal(luminanceTextColor(backgroundGradientLuminance({ color1: '#ffffff', color2: '#ffffff' }), 0), '#000000');
  assert.equal(luminanceTextColor(backgroundGradientLuminance({ color1: '#000000', color2: '#000000' }), 0), '#ffffff');
  assert.equal(luminanceTextColor(backgroundGradientLuminance({ color1: '#ffffff', color2: '#ffffff' }), .8), '#ffffff');
});
test('pattern color and strength persist, and old drafts receive the previous fixed look', () => {
  const current = event(2); const draft = initialPoster(current);
  assert.equal(draft.background.patternColor, '#ffffff'); assert.equal(draft.background.patternOpacity, .17);
  draft.background.patternColor = '#ff2266'; draft.background.patternOpacity = .55;
  const restored = restorePoster(JSON.stringify(draft), current);
  assert.equal(restored.background.patternColor, '#ff2266'); assert.equal(restored.background.patternOpacity, .55);
  delete draft.background.patternColor; delete draft.background.patternOpacity;
  const legacy = restorePoster(JSON.stringify(draft), current);
  assert.equal(legacy.background.patternColor, '#ffffff'); assert.equal(legacy.background.patternOpacity, .17);
  draft.background.patternColor = 'not-a-color'; assert.throws(() => restorePoster(JSON.stringify(draft), current));
  draft.background.patternColor = '#ffffff'; draft.background.patternOpacity = 2; assert.throws(() => restorePoster(JSON.stringify(draft), current));
});
test('pattern density persists, defaults to 1x for old drafts, and rejects out-of-range values', () => {
  const current = event(2); const draft = initialPoster(current);
  assert.equal(draft.background.patternDensity, 1);
  draft.background.patternDensity = 2.4;
  assert.equal(restorePoster(JSON.stringify(draft), current).background.patternDensity, 2.4);
  delete draft.background.patternDensity;
  assert.equal(restorePoster(JSON.stringify(draft), current).background.patternDensity, 1);
  draft.background.patternDensity = 5; assert.throws(() => restorePoster(JSON.stringify(draft), current));
});
test('all twelve patterns are valid restorable values, including the two newest', () => {
  const current = event(2); const draft = initialPoster(current);
  assert.equal(patterns.length, 12);
  assert.ok(patterns.includes('Höhenlinien')); assert.ok(patterns.includes('Marmor'));
  for (const pattern of patterns) {
    draft.background.pattern = pattern;
    assert.equal(restorePoster(JSON.stringify(draft), current).background.pattern, pattern);
  }
});
test('stroke, chaos and blend mode persist, default to the previous fixed look, and reject bad values', () => {
  const current = event(2); const draft = initialPoster(current);
  assert.equal(draft.background.patternStroke, 1); assert.equal(draft.background.patternChaos, 1); assert.equal(draft.background.patternBlend, 'source-over');
  draft.background.patternStroke = 2.2; draft.background.patternChaos = 1.7; draft.background.patternBlend = 'multiply';
  const restored = restorePoster(JSON.stringify(draft), current);
  assert.equal(restored.background.patternStroke, 2.2); assert.equal(restored.background.patternChaos, 1.7); assert.equal(restored.background.patternBlend, 'multiply');
  delete draft.background.patternStroke; delete draft.background.patternChaos; delete draft.background.patternBlend;
  const legacy = restorePoster(JSON.stringify(draft), current);
  assert.equal(legacy.background.patternStroke, 1); assert.equal(legacy.background.patternChaos, 1); assert.equal(legacy.background.patternBlend, 'source-over');
  draft.background.patternStroke = 10; assert.throws(() => restorePoster(JSON.stringify(draft), current));
  draft.background.patternStroke = 1; draft.background.patternChaos = -1; assert.throws(() => restorePoster(JSON.stringify(draft), current));
  draft.background.patternChaos = 1; draft.background.patternBlend = 'hue'; assert.throws(() => restorePoster(JSON.stringify(draft), current));
});
