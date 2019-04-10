# kparser
incomplete conversion tool between scml and kanim

tried to better understand kanim and kbuild file formats
not really all that succesful
mostly correctly can convert kanim to scml file but seems to get positions and angles slightly wrong.
additionally kanim has some way to instantly rotate without interpolation that doesn't get mirrored in scml
gave up on converting scml to kanim since the bugs with kanim->scml make it clear that I don't understand the file format well enough.
data structure classes have mostly descriptive names for data fields found in the binary files.
use dotpeek on the main game binary to see how kanim files are parsed there - gives some insight into how different parameters are used.
however it is difficult to properly search through decompiled binary dll to determine flow of kanim loading and processing (ex.
BILD file data contains information about the pivot location of each frame but the pivot property of the animation manager is not set
during this load process and I couldn't figure out when it does so).
it is likely possible without too much additional work to export scml to kanim in the same broken way that this reads kanim to scml but
this would mean any custom assets would display in game wrong.

seems likely that team behind kanim will be publishing tool for dealing with format so futher work to understand file format may be unnecessary.
