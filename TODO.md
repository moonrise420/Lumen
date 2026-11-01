# Lumen TODO (for a later / public update)

## Recents: last-frame thumbnails
- Show each recent app's last frame instead of its icon.
- System app: list tasks (getRecentTasks → taskId), then ActivityTaskManager.getTaskSnapshot(taskId)
  → HardwareBuffer → Bitmap.wrapHardwareBuffer(), via reflection (hidden APIs, API 30).
- Falls back to the app icon/banner when no snapshot (secure windows e.g. Netflix return blank).
- Needs on-device testing per app.

## Debloat menu (opt-in)
- A screen in Advanced listing known-safe bloat to remove.
- Use `pm uninstall --user 0` (reversible with `pm install-existing`, not a hard wipe).
- Never touch the stock launcher (it is the fallback when Lumen is uninstalled).
- "Restore all" button. Clear warnings. Curated list per firmware (start from the box audit).

## Nice-to-have
- Light themes (Zen, Landhaus): the clock/date sits on the photo and is low-contrast on bright
  spots. Give light themes a lighter text/shadow or a subtle scrim behind the clock.
