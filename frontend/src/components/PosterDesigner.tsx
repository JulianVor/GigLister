"use client";

import { useEffect, useRef, useState, type PointerEvent } from "react";
import { detectLogoTransparency, type LogoTransparency } from "@/lib/logo-transparency";
import type { EventResponse } from "@/lib/types";
import { initialPoster, hitLayer, patterns, blendModes, restorePoster, POSTER_WIDTH as W, POSTER_HEIGHT as H, type PosterDraft } from "@/lib/poster";
import { drawPoster, suggestTextColor, type PosterImages, type PosterFonts } from "@/lib/poster-renderer";
import { MAX_UPLOAD_SIZE_BYTES, MAX_UPLOAD_SIZE_MB } from "@/lib/upload";

const button = "border border-line px-3 py-2 font-meta text-sm hover:border-fg disabled:opacity-40 disabled:cursor-not-allowed";
const primary = `${button} border-accent bg-accent text-accent-fg`;
const clamp = (n: number, a: number, b: number) => Math.min(b, Math.max(a, n));
const blendLabels: Record<PosterDraft["background"]["patternBlend"], string> = { "source-over": "Normal", multiply: "Multiplizieren", screen: "Negativ multiplizieren", overlay: "Ineinanderkopieren", difference: "Differenz", "color-dodge": "Abwedeln", exclusion: "Ausschluss" };
type Point = { x: number; y: number };
function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => { const image = new Image(); image.onload = () => resolve(image); image.onerror = () => reject(Error("Bild konnte nicht geladen werden.")); image.src = src; });
}

export function PosterDesigner({ event }: { event: EventResponse }) {
  const [draft, setDraft] = useState(() => initialPoster(event));
  const [step, setStep] = useState<"background" | "design">("background");
  const [selected, setSelected] = useState("background");
  const [assets, setAssets] = useState<{ images: PosterImages; transparency: Map<string, LogoTransparency>; fonts: PosterFonts; source: string | null; missing: string[] } | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [imageBusy, setImageBusy] = useState(false);
  const [retry, setRetry] = useState(0);
  const [undoCount, setUndoCount] = useState(0);
  const history = useRef<PosterDraft[]>([]);
  const canvas = useRef<HTMLCanvasElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const pointers = useRef(new Map<number, Point>());
  const gesture = useRef<{ draft: PosterDraft; points: Point[]; target: string } | null>(null);
  const storageKey = `giglister-poster-v1-${event.id}`;
  const activeLayer = draft.layers.find(layer => layer.id === selected);
  const current = activeLayer ?? draft.background;
  const ready = assets !== null && assets.source === draft.background.image && (!draft.background.image || assets.images.has(draft.background.image));

  useEffect(() => {
    let cancelled = false;
    async function load() {
      const images: PosterImages = new Map(); const transparency = new Map<string, LogoTransparency>(); const missing: string[] = [];
      await Promise.all(event.bands.filter(band => band.logoUrl).map(async band => {
        try { const image = await loadImage(`/konzerte/${event.id}/plakat/bild?band=${band.id}`); images.set(band.logoUrl!, image); transparency.set(band.logoUrl!, detectLogoTransparency(image)); }
        catch { missing.push(band.name); }
      }));
      if (draft.background.image) {
        try { images.set(draft.background.image, await loadImage(draft.background.image)); }
        catch { if (!cancelled) setMessage("Der Hintergrund konnte nicht geladen werden. Bitte wähle das Bild erneut."); }
      }
      const style = getComputedStyle(document.documentElement);
      const fonts = { display: style.getPropertyValue("--font-display-family").trim() || "Arial", meta: style.getPropertyValue("--font-meta-family").trim() || "Arial" };
      await Promise.all([document.fonts.load(`32px ${fonts.display}`), document.fonts.load(`32px ${fonts.meta}`)]).catch(() => undefined);
      if (!cancelled) setAssets({ images, transparency, fonts, source: draft.background.image, missing });
    }
    void load();
    return () => { cancelled = true; };
  }, [event, draft.background.image, retry]);

  useEffect(() => {
    if (canvas.current && assets) drawPoster(canvas.current, draft, assets.images, assets.fonts, step === "design" ? selected : undefined);
  }, [draft, assets, selected, step]);

  function checkpoint() { history.current = [...history.current.slice(-19), draft]; setUndoCount(history.current.length); }
  function change(next: PosterDraft, save = true) { if (save) checkpoint(); setDraft(next); }
  function undo() { const previous = history.current.pop(); if (previous) setDraft(previous); setUndoCount(history.current.length); }
  function transform(values: Partial<{ x: number; y: number; scale: number; rotation: number }>, save = true) {
    change(selected === "background" ? { ...draft, background: { ...draft.background, ...values } } : { ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, ...values } : layer) }, save);
  }
  function point(e: PointerEvent<HTMLCanvasElement>): Point { const rect = e.currentTarget.getBoundingClientRect(); return { x: (e.clientX - rect.left) / rect.width * W, y: (e.clientY - rect.top) / rect.height * H }; }
  function down(e: PointerEvent<HTMLCanvasElement>) {
    if (step !== "design" || (e.pointerType === "mouse" && e.button !== 0)) return;
    e.preventDefault(); const p = point(e); e.currentTarget.setPointerCapture(e.pointerId);
    let target = gesture.current?.target ?? selected;
    if (pointers.current.size === 0) { checkpoint(); target = [...draft.layers].reverse().find(layer => hitLayer(layer, p.x, p.y))?.id ?? "background"; setSelected(target); }
    pointers.current.set(e.pointerId, p);
    gesture.current = { draft, points: [...pointers.current.values()], target };
  }
  function move(e: PointerEvent<HTMLCanvasElement>) {
    const start = gesture.current;
    if (!start || !pointers.current.has(e.pointerId)) return;
    e.preventDefault(); pointers.current.set(e.pointerId, point(e));
    const now = [...pointers.current.values()]; const before = start.points;
    const target = start.draft.layers.find(layer => layer.id === start.target) ?? start.draft.background;
    let dx = now[0].x - before[0].x, dy = now[0].y - before[0].y, scale = target.scale, rotation = target.rotation;
    if (now.length >= 2 && before.length >= 2) {
      const distance = (p: Point[]) => Math.hypot(p[1].x - p[0].x, p[1].y - p[0].y);
      const angle = (p: Point[]) => Math.atan2(p[1].y - p[0].y, p[1].x - p[0].x);
      scale = clamp(target.scale * distance(now) / Math.max(1, distance(before)), .4, start.target === "background" ? 5 : 3);
      const radians = angle(now) - angle(before); rotation = clamp(target.rotation + radians * 180 / Math.PI, -180, 180);
      const bx = (before[0].x + before[1].x) / 2, by = (before[0].y + before[1].y) / 2;
      const nx = (now[0].x + now[1].x) / 2, ny = (now[0].y + now[1].y) / 2;
      const factor = scale / target.scale, vx = target.x - bx, vy = target.y - by;
      dx = nx + (vx * Math.cos(radians) - vy * Math.sin(radians)) * factor - target.x;
      dy = ny + (vx * Math.sin(radians) + vy * Math.cos(radians)) * factor - target.y;
    }
    const values = { x: clamp(target.x + dx, -W, W * 2), y: clamp(target.y + dy, -H, H * 2), scale, rotation };
    setDraft(start.target === "background" ? { ...start.draft, background: { ...start.draft.background, ...values } } : { ...start.draft, layers: start.draft.layers.map(layer => layer.id === start.target ? { ...layer, ...values } : layer) });
  }
  function up(e: PointerEvent<HTMLCanvasElement>) { pointers.current.delete(e.pointerId); if (pointers.current.size) gesture.current = { draft, points: [...pointers.current.values()], target: gesture.current?.target ?? selected }; else gesture.current = null; }

  async function chooseFile(file?: File) {
    if (!file) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) { setMessage("Bitte ein JPG-, PNG- oder WebP-Bild auswählen."); return; }
    if (file.size > MAX_UPLOAD_SIZE_BYTES) { setMessage(`Das Bild darf höchstens ${MAX_UPLOAD_SIZE_MB} MB groß sein.`); return; }
    setImageBusy(true); setMessage(null);
    try {
      const data = await new Promise<string>((resolve, reject) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result)); reader.onerror = reject; reader.readAsDataURL(file); });
      const image = await loadImage(data);
      if (image.naturalWidth * image.naturalHeight > 60_000_000) throw Error("Bitte ein Bild mit höchstens 60 Megapixeln wählen.");
      change({ ...draft, background: { ...draft.background, image: data, scale: 1, x: W / 2, y: H / 2, rotation: 0 } });
      setSelected("background");
    } catch (error) { setMessage(error instanceof Error ? error.message : "Das Bild konnte nicht gelesen werden."); }
    finally { setImageBusy(false); if (fileInput.current) fileInput.current.value = ""; }
  }
  async function download() {
    if (!assets || !ready) return;
    setBusy(true); setMessage(null);
    try {
      await document.fonts.ready;
      const output = document.createElement("canvas"); output.width = 2480; output.height = 3508;
      drawPoster(output, draft, assets.images, assets.fonts);
      const blob = await new Promise<Blob>((resolve, reject) => output.toBlob(value => value ? resolve(value) : reject(Error("Export fehlgeschlagen.")), "image/png"));
      const url = URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = `giglister-${event.id}-${event.date}.png`; link.click(); setTimeout(() => URL.revokeObjectURL(url), 60000);
      setMessage("Dein Plakat wurde als PNG heruntergeladen (2480 × 3508 Pixel). Für A4 beim Drucken auf die Seite einpassen.");
    } catch { setMessage("Das Plakat konnte nicht exportiert werden. Bitte die Bilder erneut laden und noch einmal versuchen."); }
    finally { setBusy(false); }
  }
  function saveDraft() { try { localStorage.setItem(storageKey, JSON.stringify(draft)); setMessage("Entwurf in diesem Browser gespeichert."); } catch { setMessage("Nicht genug Browserspeicher. Bitte ein kleineres Hintergrundbild verwenden oder das Plakat direkt herunterladen."); } }
  function loadDraft() {
    try { const stored = localStorage.getItem(storageKey); if (!stored) throw Error("Für dieses Konzert ist hier noch kein Entwurf gespeichert."); change(restorePoster(stored, event)); setStep("design"); setMessage("Entwurf geladen. Konzertdaten wurden aktualisiert."); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Der Entwurf konnte nicht geladen werden."); }
  }
  const backgroundControls = <div className="space-y-4">
    <p className="font-meta text-xs uppercase tracking-widest text-muted">Deine Bühne</p>
    <h2 className="font-display text-2xl">Der Hintergrund.</h2>
    <p className="text-sm text-muted">Dein eigenes Bild oder ein Muster in euren Farben.</p>
    <input ref={fileInput} type="file" accept="image/png,image/jpeg,image/webp" className="sr-only" aria-label="Hintergrundbild auswählen" onChange={e => void chooseFile(e.target.files?.[0])} />
    <button type="button" className={button + " w-full"} disabled={imageBusy} onClick={() => fileInput.current?.click()}>{imageBusy ? "Bild wird geladen …" : "Eigenes Bild auswählen"}</button>
    <p className="text-xs text-muted">JPG, PNG oder WebP · bis {MAX_UPLOAD_SIZE_MB} MB. Das Bild bleibt in deinem Browser.</p>
    {draft.background.image && <button className={button} onClick={() => change({ ...draft, background: { ...draft.background, image: null } })}>Stattdessen Muster verwenden</button>}
    {!draft.background.image && <>
      <div className="grid grid-cols-2 gap-3">{(["color1", "color2"] as const).map((key, index) => <label key={key} className="font-meta text-sm">Farbe {index + 1}<input className="mt-1 h-11 w-full cursor-pointer border border-line" aria-label={`Farbe ${index + 1}`} type="color" value={draft.background[key]} onChange={e => change({ ...draft, background: { ...draft.background, [key]: e.target.value } })} /></label>)}</div>
      <label className="block font-meta text-sm">Muster<select aria-label="Muster" className="input mt-1" value={draft.background.pattern} onChange={e => change({ ...draft, background: { ...draft.background, pattern: e.target.value as PosterDraft["background"]["pattern"] } })}>{patterns.map(pattern => <option key={pattern}>{pattern}</option>)}</select></label>
      <label className="block font-meta text-sm">Musterfarbe<input aria-label="Musterfarbe" type="color" className="mt-1 h-10 w-full cursor-pointer" value={draft.background.patternColor} onChange={e => change({ ...draft, background: { ...draft.background, patternColor: e.target.value } })} /></label>
      <label className="block font-meta text-sm">Musterstärke · {Math.round(draft.background.patternOpacity * 100)} %<input aria-label="Musterstärke" className="mt-2 w-full accent-[var(--accent)]" type="range" min="0.05" max="0.9" step="0.01" value={draft.background.patternOpacity} onChange={e => change({ ...draft, background: { ...draft.background, patternOpacity: Number(e.target.value) } })} /></label>
      <label className="block font-meta text-sm">Musterdichte · {draft.background.patternDensity.toFixed(1)}×<input aria-label="Musterdichte" className="mt-2 w-full accent-[var(--accent)]" type="range" min="0.3" max="3" step="0.1" value={draft.background.patternDensity} onChange={e => change({ ...draft, background: { ...draft.background, patternDensity: Number(e.target.value) } })} /></label>
      <label className="block font-meta text-sm">Strichstärke · {draft.background.patternStroke.toFixed(1)}×<input aria-label="Strichstärke" className="mt-2 w-full accent-[var(--accent)]" type="range" min="0.3" max="3" step="0.1" value={draft.background.patternStroke} onChange={e => change({ ...draft, background: { ...draft.background, patternStroke: Number(e.target.value) } })} /></label>
      <label className="block font-meta text-sm">Chaos · {draft.background.patternChaos.toFixed(1)}<input aria-label="Chaos" className="mt-2 w-full accent-[var(--accent)]" type="range" min="0" max="2" step="0.1" value={draft.background.patternChaos} onChange={e => change({ ...draft, background: { ...draft.background, patternChaos: Number(e.target.value) } })} /></label>
      <label className="block font-meta text-sm">Mischmodus<select aria-label="Mischmodus" className="input mt-1" value={draft.background.patternBlend} onChange={e => change({ ...draft, background: { ...draft.background, patternBlend: e.target.value as PosterDraft["background"]["patternBlend"] } })}>{blendModes.map(mode => <option key={mode} value={mode}>{blendLabels[mode]}</option>)}</select></label>
      <button className={button} onClick={() => { const palettes = [["#171f2c", "#c84b24"], ["#0b3535", "#9a7b27"], ["#261533", "#ad355a"], ["#111111", "#626262"]]; const colors = palettes[Math.floor(Math.random() * palettes.length)]; change({ ...draft, background: { ...draft.background, color1: colors[0], color2: colors[1], seed: Math.floor(Math.random() * 1e6) } }); }}>Neue Farbstimmung</button>
    </>}
    <label className="block font-meta text-sm">Abdunkeln · {Math.round(draft.background.dim * 100)} %<input className="mt-2 w-full accent-[var(--accent)]" type="range" min="0" max="0.85" step="0.01" value={draft.background.dim} onChange={e => change({ ...draft, background: { ...draft.background, dim: Number(e.target.value) } })} /></label>
  </div>;

  return <section className="mt-5">
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div><p className="mb-2 font-meta text-xs uppercase tracking-[.22em] text-accent">Vom Konzert zum Plakat</p><h1 className="font-display text-3xl sm:text-4xl">Plakat-Designer</h1><p className="mt-2 text-sm text-muted">{step === "background" ? "01 Hintergrund wählen → 02 Plakat gestalten" : "02 Plakat gestalten · auswählen, verschieben, verfeinern"}</p></div>
      <button className={button} onClick={loadDraft}>Entwurf laden</button>
    </div>
    {message && <p role="status" className="mb-4 border border-accent bg-surface p-3 text-sm">{message}</p>}
    {assets?.missing.length ? <div role="status" className="mb-4 border border-line p-3 text-sm">Logo nicht verfügbar: {assets.missing.join(", ")}. Stattdessen wird der Bandname verwendet. <button className="underline" onClick={() => setRetry(value => value + 1)}>Bilder erneut laden</button></div> : null}
    <div className="grid items-start gap-6 md:grid-cols-[minmax(0,1fr)_300px]">
      <div className="min-w-0 border border-line bg-[#22201e] p-3 sm:p-5 md:sticky md:top-4">
        <div className="mx-auto max-w-[560px] shadow-2xl">
          <canvas ref={canvas} width={1000} height={1414} aria-label="Plakatvorschau" tabIndex={step === "design" ? 0 : -1}
            className={`block h-auto w-full ${step === "design" ? "touch-none cursor-move" : ""}`} onPointerDown={down} onPointerMove={move} onPointerUp={up} onPointerCancel={up} onLostPointerCapture={up}
            onKeyDown={e => { const directions: Record<string, [number, number]> = { ArrowLeft: [-10, 0], ArrowRight: [10, 0], ArrowUp: [0, -10], ArrowDown: [0, 10] }; if (step === "design" && directions[e.key]) { e.preventDefault(); const [x, y] = directions[e.key]; transform({ x: clamp(current.x + x, -W, W * 2), y: clamp(current.y + y, -H, H * 2) }); } }}>Plakat für {event.title || event.bands.map(band => band.name).join(", ")}: {event.location.name}, {event.date} {event.startTime?.slice(0, 5)}</canvas>
        </div>
        <p className="mt-3 text-center font-meta text-xs tracking-wide text-white/60">A4 HOCHFORMAT · {step === "design" ? "Ebenen mit Maus, Touch oder Pfeiltasten verschieben" : "Vorschau mit euren Konzertdaten"}</p>
      </div>
      <aside className="space-y-5 border border-line bg-surface p-5">
        {step === "background" ? <>{backgroundControls}<button className={primary + " w-full"} disabled={!ready || imageBusy} onClick={() => {
          if (assets) {
            const suggestion = suggestTextColor(draft.background, assets.images);
            const layers = draft.layers.map(layer => layer.color === "#ffffff" ? { ...layer, color: suggestion } : layer);
            if (layers.some((layer, index) => layer.color !== draft.layers[index].color)) change({ ...draft, layers }, false);
          }
          setStep("design"); setSelected(draft.layers[0]?.id ?? "background");
        }}>Weiter zum Designer →</button></> : <>
          <div className="flex gap-2"><button className={button} onClick={() => setStep("background")}>← Hintergrund</button><button className={button} disabled={!undoCount} onClick={undo}>Rückgängig</button></div>
          <h2 className="font-display text-xl">Dein Line-up. Dein Look.</h2>
          <label className="block font-meta text-sm">Ebene auswählen<select aria-label="Ebene auswählen" className="input mt-1" value={selected} onChange={e => setSelected(e.target.value)}><option value="background">Hintergrund</option>{draft.layers.map(layer => <option key={layer.id} value={layer.id}>{layer.label}</option>)}</select></label>
          <p className="text-xs text-muted">Direkt im Plakat verschieben. Mit zwei Fingern zoomen und drehen oder die Regler verwenden.</p>
          <label className="block font-meta text-sm">Größe · {Math.round(current.scale * 100)} %<input aria-label="Größe" className="mt-2 w-full accent-[var(--accent)]" type="range" min="0.4" max={selected === "background" ? "5" : "3"} step="0.01" value={current.scale} onChange={e => transform({ scale: Number(e.target.value) })} /></label>
          <label className="block font-meta text-sm">Drehung · {Math.round(current.rotation)}°<input aria-label="Drehung" className="mt-2 w-full accent-[var(--accent)]" type="range" min="-180" max="180" value={current.rotation} onChange={e => transform({ rotation: Number(e.target.value) })} /></label>
          {activeLayer && <>
            <label className="block font-meta text-sm">Textfarbe<input aria-label="Textfarbe" type="color" className="mt-1 h-10 w-full" value={activeLayer.color} onChange={e => change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, color: e.target.value } : layer) })} /></label>
            {activeLayer.kind === "band" && activeLayer.logoUrl && <label className="block font-meta text-sm">Logo-Darstellung<select aria-label="Logo-Darstellung" className="input mt-1" value={activeLayer.logoMode} onChange={e => change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, logoMode: e.target.value as "original" | "white" | "black" } : layer) })}><option value="original">Originalfarben</option><option value="white">Weiß</option><option value="black">Schwarz</option></select></label>}
            {activeLayer.kind === "band" && activeLayer.logoUrl && assets?.images.has(activeLayer.logoUrl) && <div className="space-y-3 border border-line p-3">
              <p className="text-xs text-muted" role="status">{assets.transparency.get(activeLayer.logoUrl) === "transparent" ? "Logo: transparente Pixel erkannt." : assets.transparency.get(activeLayer.logoUrl) === "opaque" ? "Logo: nicht transparent. Du kannst einen Rahmen ergänzen." : "Logo-Transparenz nicht ermittelbar. Ein Rahmen ist manuell möglich."}</p>
              {(assets.transparency.get(activeLayer.logoUrl) !== "transparent" || activeLayer.logoFrame) && <>
                <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={activeLayer.logoFrame} onChange={e => change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, logoFrame: e.target.checked } : layer) })} />Rahmen um dieses Logo</label>
                {activeLayer.logoFrame && <>
                  <label className="block font-meta text-sm">Rahmenfarbe<input aria-label="Rahmenfarbe" type="color" className="mt-1 h-10 w-full" value={activeLayer.logoFrameColor} onChange={e => change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, logoFrameColor: e.target.value } : layer) })} /></label>
                  <label className="block font-meta text-sm">Rahmenstärke · {activeLayer.logoFrameWidth}<input aria-label="Rahmenstärke" type="range" min="2" max="24" step="1" className="mt-2 w-full accent-[var(--accent)]" value={activeLayer.logoFrameWidth} onChange={e => change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, logoFrameWidth: Number(e.target.value) } : layer) })} /></label>
                </>}
              </>}
            </div>}
            {activeLayer.kind !== "footer" && <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={activeLayer.box} onChange={e => change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? { ...layer, box: e.target.checked } : layer) })} />Dunkler Kasten dahinter</label>}
            {activeLayer.kind === "footer" && <label className="block font-meta text-sm">Schwarzer Kasten · {Math.round(draft.footerOpacity * 100)} %<input aria-label="Kasten-Deckkraft" type="range" min="0" max="1" step="0.01" className="mt-2 w-full accent-[var(--accent)]" value={draft.footerOpacity} onChange={e => change({ ...draft, footerOpacity: Number(e.target.value) })} /></label>}
          </>}
          <button className={button + " w-full"} onClick={() => { const original = initialPoster(event); if (selected === "background") transform({ x: W / 2, y: H / 2, rotation: 0, scale: 1 }); else change({ ...draft, layers: draft.layers.map(layer => layer.id === selected ? original.layers.find(item => item.id === selected)! : layer) }); }}>Ebene zurücksetzen</button>
          <button className={button + " w-full"} onClick={() => change({ ...draft, layers: initialPoster(event).layers })}>Line-up automatisch anordnen</button>
          <label className="block border-t border-line pt-4 font-meta text-sm">Eckenradius · {draft.cornerRadius}<input aria-label="Eckenradius" className="mt-2 w-full accent-[var(--accent)]" type="range" min="0" max="80" step="1" value={draft.cornerRadius} onChange={e => change({ ...draft, cornerRadius: Number(e.target.value) })} /></label>
          <div className="space-y-3 border-t border-line pt-4"><button className={primary + " w-full"} disabled={!ready || busy || imageBusy} onClick={() => void download()}>{busy ? "Plakat wird exportiert …" : "Plakat als PNG herunterladen"}</button><button className={button + " w-full"} onClick={saveDraft}>Entwurf speichern</button><p className="text-xs text-muted">2480 × 3508 Pixel · für A4. Entwürfe bleiben in diesem Browser; das Konzert wird nicht verändert.</p></div>
        </>}
        {!ready && <p className="font-meta text-sm" role="status">Bilder und Schriften werden geladen …</p>}
      </aside>
    </div>
  </section>;
}
