function searchSongs(request) {
  return { songs: [{ id: "test-1", title: request.title, artist: request.artist || "", duration: request.duration || 0, fields: { album: "Test Album" }, internal: { ref: "test-ref-123" } }] };
}
function getLyrics(request) {
  return { lines: [{ text: "Hello world", startMs: 0, endMs: 1000, words: [{ text: "Hello", startMs: 0, endMs: 400 }, { text: " world", startMs: 500, endMs: 1000 }] }], rawPlainLrc: "[00:00.00]Hello world", translated: "\u4f60\u597d\u4e16\u754c" };
}
function searchCovers(request) {
  return { covers: [{ url: "https://example.com/cover.jpg", width: 800, height: 800 }] };
}
