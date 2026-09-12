# Figma asset provenance

The Automotive Expanded vector controls in `car/presentation/src/main/res/drawable`
come from the TidePlayer Automotive Core UI Figma file:

- File key: `5NO5nvLFdd2jPTwHmnJE8A`
- Home Dark: node `1:2`
- Now Playing Dark: node `1:65`
- Queue Dark: node `1:122`
- Album Detail Dark: node `977:944`
- Artist Detail Dark: node `977:1189`

The implementation queried design context for those frames, exported the referenced
SVG icon assets, and mechanically converted their SVG path data and view boxes to
Android VectorDrawable XML. Paths were not visually redrawn. Signed export URLs are
temporary and are intentionally excluded from source control; the Figma file key and
node IDs above are the durable traceability record.

The resulting resources cover shell navigation, search, close/back/collapse,
previous/play/pause/next, queue, favorite, overflow, shuffle and repeat controls.
Light and dark rendering tint the same source geometry with semantic theme colors.
