# Automotive search and page-state contract

Figma has no dedicated Expanded Search, Loading, Empty, Error, or dialog frames. This file closes the implementation gap for Search and non-modal page states without claiming a Figma visual match.

- Search remains inside the Expanded shell and uses the same content origin, type roles, semantic surfaces, focus border, touch targets, and song-row component as the mapped Library screens.
- The query field is a single-line, focusable control. A blank query shows guidance. A non-blank query is debounced before calling the shared `SearchRepository`; results never come from a car-local index.
- A playable result calls the shared `PlaybackController`. Invalid results remain visible but disabled.
- Loading, Empty, and Error are mutually exclusive page states centered in the content pane with `title` typography and `textSecondary`. Error text must expose the failure instead of replacing it with demo content.
- These states keep the navigation rail and mini player available. Modal dialogs remain deferred until a node or separate dialog contract is supplied.
