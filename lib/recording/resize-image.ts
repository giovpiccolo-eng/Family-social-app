"use client";

// Resize an image to fit within maxEdge pixels on the longest side and encode
// as JPEG. Per brief §2.2 / §4.6: 1280px max edge, quality 0.82.
export async function resizeImage(
  file: File,
  maxEdge = 1280,
  quality = 0.82,
): Promise<Blob> {
  const bitmap = await createImageBitmap(file);
  const ratio = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
  const w = Math.round(bitmap.width * ratio);
  const h = Math.round(bitmap.height * ratio);

  const canvas =
    typeof OffscreenCanvas !== "undefined"
      ? new OffscreenCanvas(w, h)
      : Object.assign(document.createElement("canvas"), { width: w, height: h });

  const ctx = (canvas as HTMLCanvasElement | OffscreenCanvas).getContext("2d");
  if (!ctx) throw new Error("Canvas 2D context unavailable");
  (ctx as CanvasRenderingContext2D).drawImage(bitmap, 0, 0, w, h);
  bitmap.close?.();

  if ("convertToBlob" in canvas) {
    return await (canvas as OffscreenCanvas).convertToBlob({
      type: "image/jpeg",
      quality,
    });
  }
  return await new Promise<Blob>((resolve, reject) => {
    (canvas as HTMLCanvasElement).toBlob(
      (b) => (b ? resolve(b) : reject(new Error("Image encode failed"))),
      "image/jpeg",
      quality,
    );
  });
}
