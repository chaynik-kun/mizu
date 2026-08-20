# Mizu release device test checklist

## Install / update

- [ ] Clean install
- [ ] Update over a previous Mizu build signed with the same key
- [ ] Settings preserved after update and restart

## Login

- [ ] Normal Navidrome/OpenSubsonic login
- [ ] Slow connection
- [ ] Offline startup
- [ ] Reconnect after network recovery

## Library

- [ ] Albums
- [ ] Artists
- [ ] Songs
- [ ] Playlists
- [ ] Search
- [ ] Fast scrolling in a large library

## Playback

- [ ] Play/pause
- [ ] Next/previous
- [ ] Seek
- [ ] Shuffle and repeat
- [ ] Queue edits
- [ ] Screen-off playback

## Pre-buffer

- [ ] Normal automatic transition
- [ ] Slow network
- [ ] Queue changed before transition

## Mini Player

- [ ] Full background at 0/25/50/75/100%
- [ ] Bottom bar at 0/25/50/75/100%
- [ ] Track change and seek
- [ ] Pause, buffering, and unknown duration
- [ ] Tap, swipe, play/pause, and next controls
- [ ] Preference survives app restart
- [ ] Local and DLNA playback

## Now Playing

- [ ] Progress/time/technical-info layout remains stable
- [ ] Lyrics
- [ ] Equalizer
- [ ] Sleep timer
- [ ] Queue

## Equalizer

- [ ] Enable/disable
- [ ] Bands and presets
- [ ] Compact sheet closes with Back/dismiss
- [ ] All Now Playing actions work after closing it repeatedly

## DLNA

- [ ] Discover and connect
- [ ] Play and disconnect
- [ ] Return to local playback

## GPL / remote playback

- [ ] App works without Google Play services Cast Framework
- [ ] No Google Cast button or target remains
- [ ] DLNA still works

## Now Playing actions

- [ ] All actions visible by default
- [ ] Hide one action
- [ ] Hide several actions
- [ ] Hide all actions; layout remains clean
- [ ] Settings survive restart

## Developer settings

- [ ] Visible in debug build
- [ ] Absent in release build

## Equalizer touch

- [ ] Sliders are easier to grab
- [ ] Adjacent bands do not change accidentally
- [ ] Vertical drag works without moving the sheet
- [ ] EQ sound changes and presets still work

## Crash hardening

- [ ] Missing database entity produces a controlled error, not an NPE
- [ ] Permission request before launcher registration does not crash

## Android Auto

- [ ] Browse and configured sections
- [ ] Search/play from search
- [ ] Playback controls
- [ ] Disconnect and reconnect

## Network

- [ ] Wi-Fi and mobile network
- [ ] Wi-Fi to mobile transition
- [ ] Network loss and recovery

## Cache / offline

- [ ] Cached track
- [ ] Downloaded track
- [ ] Offline playback

## Long session

- [ ] One hour or longer playback
- [ ] Screen off
- [ ] No obvious heat or battery drain
- [ ] No crash
