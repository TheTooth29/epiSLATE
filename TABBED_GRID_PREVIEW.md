# Tabbed Grid comparison and recovery

The unchanged starting source is frozen at branch `backup/tabbed-grid-original-2026-11-17`, commit `0592e9e6b1ab4937956118b70353135f882f204c`.

The Test app exposes **Settings → Appearance → Tabbed Grid preview → Layout**:

- Original: the original TabbedGridContent implementation, retained unchanged apart from its function name.
- Two full rows: six columns, scroll one row when focus crosses a visible edge.
- Three full rows: eight columns, smaller cards, the same edge behavior.
- Page by page: six columns, advance or retreat in groups of two rows.
- Centered preview: selected row centered, neighbouring rows partially visible and dimmed.

Original is the default. Preview settings and saved positions use a separate Test-only preference file, keyed by collection, tab and choice. Switching choices does not erase the other choices' positions. The regular package and Follow Layout collections always use the original implementation; the selector is hidden in the regular app.

To restore the display, select **Original**. To recover the complete pre-experiment source, check out the frozen backup branch on a fresh work branch. Do not reset, overwrite or delete that checkpoint. No stable branch, package identity, signing or release distribution changes are part of this experiment.

Device comparison should check D-pad movement in both directions, first and last rows, multiple tabs, opening and returning from details, loading more results, and switching each choice back to Original. Compilation and policy tests cannot establish the final TV appearance or focus behavior.
