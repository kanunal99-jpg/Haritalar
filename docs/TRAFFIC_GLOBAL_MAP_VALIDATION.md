# Global Live Traffic Map Validation

Date: 2026-09-10

## Purpose

The live traffic layer must be visible independently of route calculation. A user should be able to open the map, zoom to a city road area, and see real TomTom Flow Segment Data rendered on the visible roads without selecting a destination or starting navigation.

## Implementation status

- Visible-map traffic refresh is independent of route selection.
- MapLibre renders provider-returned segment geometry using the existing verified severity colors.
- TomTom access remains through the canonical traffic provider/factory path; no fake traffic is generated.
- Viewport sampling is bounded (3x3 normally, 4x4 at zoom 15+), refresh is throttled to 60 seconds, and zoom below 11 does not query flow data.
- Provider point responses are cached by the existing TomTom provider cache.

## Acceptance test

1. Install the newly produced APK from the successful CI run for the current main HEAD.
2. Open the app without creating a route.
3. Zoom to a road-dense area at approximately zoom 13-16.
4. Pan the map and wait for the traffic refresh.
5. Observe real green/yellow/orange/red traffic segments on road geometry.
6. Repeat with a route selected and confirm route traffic remains visible.

## User validation

Status: PENDING — device observation is required. CI success alone does not close this feature.
