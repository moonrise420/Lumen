# Lumen TODO (for a later / public update)

## Placement presets
- Save keystone + orientation as named presets (Table / Ceiling / Shelf) and switch in one tap.
- Keystone lives in system properties (`persist.sys.keystone.*` corner points + `keystoneAngle`),
  orientation in `user_rotation`. The idea: snapshot those into a preset and restore them on demand.
- Needs careful on-device testing (a wrong write misaligns the live picture, so always save the
  current values first and keep a one-tap restore).

## Debloat menu (opt-in)
- A screen in Advanced listing known-safe bloat to remove.
- Use `pm uninstall --user 0` (reversible with `pm install-existing`, not a hard wipe).
- Never touch the stock launcher (it is the fallback when Lumen is uninstalled).
- "Restore all" button. Clear warnings. Curated list per firmware (start from the box audit).

## Nice-to-have
- Light themes (Zen, Landhaus): the clock/date sits on the photo and is low-contrast on bright
  spots. Give light themes a lighter text/shadow or a subtle scrim behind the clock.
