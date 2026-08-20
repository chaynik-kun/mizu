# External playback manual test checklist

Run every scenario with a queue of at least three tracks. Include an original FLAC and a transcoded MP3 track.

## DLNA / UPnP

Devices: smart TV, AV receiver, Volumio and Moode (where available).

- Verify that non-renderer UPnP devices do not appear.
- Discover the renderer while the picker is open.
- Transfer Local → DLNA without restarting the current track.
- Play, pause, seek, next and previous.
- Verify MP3 fallback for a renderer that does not advertise the source codec.
- Verify original FLAC is selected only when ConnectionManager Sink protocol info advertises `audio/flac`.
- Let a track end and verify Mizu advances the renderer queue.
- Transfer DLNA → Local and compare positions.
- Lock the phone and background/foreground Mizu.
- Disconnect/reconnect Wi-Fi.
- Reboot the renderer and verify the disconnected/error state.
- Confirm a natural `PLAYING → STOPPED` transition advances once; repeated STOPPED responses must not skip two tracks.
- Confirm pressing Stop does not trigger automatic queue advance.
- Test an HTTPS server with a publicly trusted certificate.
- Test a LAN-only Navidrome URL reachable by the renderer.
- Verify that inaccessible, VPN-only or localhost URLs produce a friendly playback error.

## Security

- Inspect debug logs and confirm that auth token, salt and password query values are redacted.
- Confirm that no TLS trust-all or hostname-verifier override is installed.
- Confirm that local playback cache files are never exposed as renderer URLs.
