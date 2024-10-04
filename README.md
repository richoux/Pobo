# pobo

_pobo_ is an implementation for Android of the abstract board game Gekitai² (aka "boop." for its commercial version). In this 2-player game, you must align 3 small pieces to promote them into large ones, and align 3 large pieces to win. The trick is that a piece pushes away the other ones around when played!

The  app is  free,  ad-free, and  open  source under  the  GNU GPL  v3
License. It can be played by two  players on the same device, or alone
against a strong, cutting-edge AI.

## Download

[<img alt="Get it on Google Play" height="100" src="https://github.com/richoux/pobo/wiki/images/GooglePlay.png">](https://play.google.com/store/apps/details?id=fr.richoux.pobo)

You can also get the APK file of the [latest release](https://github.com/richoux/pobo/releases/latest).

## How to play

The game starts with  an empty board.  Players have a  pool of 8 small
pieces, and place one piece alternately.

When a  piece is placed, it  pushes away all adjacent  pieces from one
square, except if a piece is blocked by another piece.

![](https://github.com/richoux/pobo/wiki/images/pobo_push_01.gif)

Large pieces can push away any other pieces, but small ones cannot
push away large pieces.

![](https://github.com/richoux/pobo/wiki/images/pobo_push_02.gif)

When a piece is pushed out of the board, it returns into its player's
pool.

When 3 of your pieces are aligned,  they are removed from the board at
the end of your turn, and  return into your pool. Small pieces removed
that  way are  promoted to  large pieces.  If more  than 3  pieces are
aligned, you choose  3 adjacent pieces to remove. If  you place your 8
pieces on the board,  you have to choose one piece  to remove from the
board. In case  this piece is a  small one, it is promoted  to a large
piece.

There are 2 winning conditions:
You have 3 large pieces aligned at the end of your turn.
You place your 8th large piece on the board.

That's it! Have fun!


## Code

pobo is written with Jetpack Compose and ported from
[https://github.com/bentrengrove/chess](https://github.com/bentrengrove/chess)
for Chess under MIT License.

I use Android Studio to develop  this app. This repo contains the file
project.

## AI

The AI for  solo games is based  on a combination of  Monte Carlo Tree
Search and Combinatorial Optimization  methods. Please read the [paper
on arxiv](https://arxiv.org/abs/2406.08766) for more details.

I used my Combinatorial Optimization framework [GHOST](https://github.com/richoux/GHOST)
to model and solve the inherent Combinatorial Optimization problem.

## License

This app is under the term of the GNU GPL v3 license.

## Illustrations

Ghost illustrations have been made by
[bcogwene](https://pixabay.com/users/bcogwene-1114581/) and  are under
CC0 License. Other  illustrations (AI level, icons) are  also under CC
License. 

## Screenshots

![](https://github.com/richoux/pobo/wiki/images/Screenshot_phone_01.png)
![](https://github.com/richoux/pobo/wiki/images/Screenshot_phone_02.png)
![](https://github.com/richoux/pobo/wiki/images/Screenshot_phone_03.png)
![](https://github.com/richoux/pobo/wiki/images/Screenshot_phone_04.png)

<!-- ## Screenshots -->
