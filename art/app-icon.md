# App icon

https://developer.android.com/develop/ui/views/launch/icon_design_adaptive

For SVG use 1 dp = 4 px

Recommended size of 108 dp = 432 px

So set page size:
width = 432 px
height = 432 px
scale = 1,0 (so viewbox is same size)

Add guides to help:
Only draw in inner 72 dp = 288 px
So margin of 18 dp = 72 px
Left: x = 72 px
Right: x = 360 px
Top: y = 72 px
Bottom: y = 360 px

Add safe zone circle:
Safe zone of 66 dp = 33 dp radius = 132 px
