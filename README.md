# sts-ttsgenerator

This is a `Slay the Spire` mod used to generate decks used in
`Slay the Spire: Board Game`. These can be imported to
`Tabletop Simulator` for playing.

## How to use

1. Clone this repository
2. Define your cards. See [ironclad.json](src/main/resources/ttsgenerator/cards/ironclad.json).
3. Define your card sets. See [ironcladbasic.json](src/main/resources/ttsgenerator/cardsets/ironcladbasic.json).
4. Update code.
   1. if you used a different name for cards or cards set. Modify [TtsGenerator.java](/src/main/java/io/chaofan/sts/ttsgenerator/TtsGenerator.java#L59).
   2. if you what to add your icon. Modify [ScvRenderDescriptionPatch.java](src/main/java/io/chaofan/sts/ttsgenerator/patches/ScvRenderDescriptionPatch.java#L253)
5. Run Modded Slay the Spire with your mod and ttsgenerator.
6. Wait for game exit.
7. You can find the outputs at installation path of Slay the Spire
   1. For example, on Windows, it's C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire.
   
## Other tools

If you want to customize player board. See [board.xcf](image-template/board.xcf).
You need [GIMP](https://www.gimp.org/downloads/) to open it.

## Example

See [bladegunner example](https://github.com/herbix/sts-ttsgenerator/tree/bladegunner).
