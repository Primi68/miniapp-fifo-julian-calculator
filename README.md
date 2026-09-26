# FIFO Julian Calculator

Android calculator for converting tire Julian production codes into FIFO label dates.

The app is intentionally small. It is designed for a work process where the user enters a tire code, reads the label date, and moves on to the next tire without using a paper Julian-date table or a separate calculator.

## Core behavior

After a valid conversion, the entered value stays in place until the operator edits it or presses `×`. This avoids accidental replacement when a worker pauses briefly before the next entry. The `×` button always clears the entry completely.

### Julian number to FIFO date

| Input | Output |
| --- | --- |
| `001` | `1/01` |
| `243` | `8/31` |
| `250` | `9/07` |
| `258` | `9/15` |
| `365` | `12/31` |
| `26254` | `9/11` |

- A three-digit entry is a Julian day number.
- A five-digit entry uses `YYDDD` format. `26254` means the 254th day of 2026.
- Three-digit entries use the current Austin year (`America/Chicago`).
- Five-digit entries use the year included in the code, including leap-year handling.

### Date to Julian number

Tap `⇄` to reverse the conversion.

| Input | Interpretation | Output |
| --- | --- | --- |
| `9 15` | September 15 | `258` |
| `9/15` | September 15 | `258` |
| `915` | September 15 | `258` |
| `11 2` | November 2 | `306` |
| `1102` | November 2 | `306` |
| `112` | January 12 | `012` |

Ambiguous three-digit dates always follow the `MDD` rule. For example, `112` always means January 12. Enter November 2 as `11 2`, `11/2`, or `1102`.

## Interface

- The upper-left text shows the Austin date and its current `YYDDD` code.
- The main result is displayed in a large label-ready `M/DD` format.
- `?` opens the input-format help dialog.
- `×` clears the current entry.
- Invalid days and dates show an error instead of a conversion.
- The app stores no history and requests no internet permission.

## Android layout note

The app must account for the system status area and on-screen keyboard instead of placing controls at fixed screen coordinates. The relevant Android concepts are:

- **Window Insets:** keep the date header below the status bar and above the navigation area.
- **IME or keyboard insets:** evaluate the available window when the numeric keyboard is open.
- **Constraint-based layout:** anchor elements to each other and to the usable screen area, rather than using a fixed y coordinate.
- **Device validation:** compare a keyboard-open and keyboard-closed screenshot on the actual target phone after each layout change.

The current implementation applies system-bar insets and uses a small vertical adjustment. The remaining UI question is the exact visual center of the FIFO result while the keyboard is open; that should be adjusted against the target phone screenshot rather than guessed from a desktop emulator.

## Short change log

| Version | Change |
| --- | --- |
| `1.0` | Initial Julian and reverse date conversion |
| `1.1` | Rebuilt with a new Android version code so update installation is reliable |
| `1.2` | Added system-bar safe-area handling to prevent overlap with the phone status bar |
| `1.3` | Shortened completed-entry auto-selection from 0.65 seconds to 0.45 seconds, so the next tire code can be entered sooner |
| `1.4-experimental.4` | Experimental adaptive layout uses Android IME insets; both conversion modes open the numeric keyboard, and automatic post-conversion selection was removed to prevent accidental replacement |

## Development

The required end-to-end release process is documented in [Mini-App Development and Release Workflow](docs/miniapp-development-and-release-workflow.md).

Open this folder in Android Studio and let it install the declared Android SDK and Gradle dependencies. The project uses:

- Android Gradle Plugin 8.7.3
- compile SDK 35
- min SDK 26
- Java source compatibility 7

Build a debug APK with:

```text
./gradlew assembleDebug
```

The resulting APK is normally located at `app/build/outputs/apk/debug/app-debug.apk`.

## Test checklist

- Confirm `250 → 9/07`.
- Confirm `26254 → 9/11`.
- Confirm `9 15 → 258` after using `⇄`.
- Confirm `112 → 012` and `11 2 → 306`.
- Confirm invalid inputs such as `000`, `2 30`, and `13 1` show an error.
- Confirm the date header does not overlap status icons.
- Confirm the result remains comfortably visible when the keyboard is open.
