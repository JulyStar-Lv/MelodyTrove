# Search UI regression evidence

- Baseline commit: `a8207cdbb46381c22d669eb252883c76d2fd90df`
- Regressed commit: `d08e0e25c8239f700d52f2f315ba86b06b9e0d1b`
- Introducing commit: `ea5b3530a23a7fe15a4ec4916a3a2ca2487d4952`

The introducing commit replaced the full `SearchDesignScreen` implementation with a delegate to `SearchScreen`.
In the delegated layout, `DesignSearchBar` is a direct child of a full-height `Column` while its internal text field uses `fillMaxSize()`. When search content is hidden, the input surface expands vertically and occupies the remaining page.

The fix restores the original design screen and keeps the current stable list-key rule through a compatibility extension.
