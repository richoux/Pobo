package fr.richoux.pobo.engine

import androidx.annotation.DrawableRes
import fr.richoux.pobo.R

private const val TAG = "pobotag Board"
private var pieceCounter: Int = 0

sealed class PieceType(val value: Byte) {
  object Po : PieceType(1) // small ones
  object Bo : PieceType(2) // big ones

  operator fun compareTo(other: PieceType): Int {
    return this.value - other.value
  }
}

sealed class Color(val value: Byte) {
  object Blue : Color(-1)
  object Red : Color(1)

  fun other(): Color {
    return if(this == Blue) Red else Blue
  }

  fun otherInByte(): Byte {
    return (-value).toByte()
  }

  override fun toString(): String {
    return if(this == Blue) "Blue" else "Red"
  }
}

private fun pieceCodeFromId(id: String): Byte {
  val chars = id.toCharArray()
  val pieceColor = when(chars[0]) {
    'B' -> Color.Blue.value
    'R' -> Color.Red.value
    else -> throw IllegalStateException("First character should be B or R")
  }
  val pieceType = when(chars[1]) {
    'P' -> PieceType.Po.value
    'B' -> PieceType.Bo.value
    else -> throw IllegalStateException("Second character should be a piece type")
  }
  return (pieceColor * pieceType).toByte()
}

val BluePo = Piece("BP", -1)
val BlueBo = Piece("BB", -2)
val RedPo = Piece("RP", 1)
val RedBo = Piece("RB", 2)

fun getBoInstanceOfColor(color: Color): Piece {
  return when(color) {
    Color.Blue -> BlueBo
    Color.Red -> RedBo
  }
}

fun getPoInstanceOfColor(color: Color): Piece {
  return when(color) {
    Color.Blue -> BluePo
    Color.Red -> RedPo
  }
}

fun getPieceInstance(color: Color, type: Byte): Piece {
  return when(type) {
    1.toByte() -> getPoInstanceOfColor(color)
    else -> getBoInstanceOfColor(color)
  }
}

data class Piece(val id: String, val code: Byte) {
  // codes:
  //  1 = Red Po
  //  2 = Red Bo
  // -1 = Blue Po
  // -2 = Blue Bo

  // types:
  // 1 = Po, i.e., small pieces
  // 2 = Bo, i.e., big pieces

  // colors:
  //  1 = Red
  // -1 = Blue

  companion object {
    fun pieceOrNullFromString(id: String?): Piece? {
      val id = id ?: return null
      val code = pieceCodeFromId(id)
      pieceCounter++
      val fullID: String = id + pieceCounter
      return Piece(fullID, code)
    }

    fun pieceFromString(id: String): Piece {
      val code = pieceCodeFromId(id)
      pieceCounter++
      val fullID: String = id + pieceCounter
      return Piece(fullID, code)
    }

    fun createPo(color: Color): Piece {
      val id: String = when(color) {
        Color.Blue -> "BP"
        Color.Red -> "RP"
      }
      pieceCounter++
      val fullID: String = id + pieceCounter
      val code = (PieceType.Po.value * color.value).toByte()
      return Piece(fullID, code)
    }

    fun createBo(color: Color): Piece {
      val id: String = when(color) {
        Color.Blue -> "BB"
        Color.Red -> "RB"
      }
      pieceCounter++
      val fullID: String = id + pieceCounter
      val code = (PieceType.Bo.value * color.value).toByte()
      return Piece(fullID, code)
    }

    fun createFromByte(color: Color, type: Byte): Piece {
      return when(type) {
        PieceType.Po.value -> createPo(color)
        else -> createBo(color)
      }
    }
  }

  fun getColor(): Color {
    return if(code > 0) Color.Red else Color.Blue
  }

  fun getType(): PieceType {
    return if(abs(code).toByte() == PieceType.Po.value) PieceType.Po else PieceType.Bo
  }

  operator fun compareTo(other: Piece): Int {
    return abs(this.code) - abs(other.code)
  }

  private fun abs(type: Byte): Int {
    return if(type > 0) type.toInt() else -type
  }

  fun isEquivalent(other: Piece?): Boolean {
    return this === other || this.code == other?.code
  }

  override fun toString(): String {
    return id
  }

  @DrawableRes
  fun imageResource(): Int {
    return when(getType()) {
      PieceType.Po -> if(getColor() is Color.Blue) R.drawable.blue_po else R.drawable.red_po
      PieceType.Bo -> if(getColor() is Color.Blue) R.drawable.blue_bo else R.drawable.red_bo
    }
  }
}

//TODO change (x,y) to (row,column), and beware of the reversed order
data class Delta(val x: Int, val y: Int)
data class Position(val x: Int, val y: Int) {
  operator fun plus(other: Position): Delta {
    return Delta(this.x + other.x, this.y + other.y)
  }

  operator fun minus(other: Position): Delta {
    return Delta(this.x - other.x, this.y - other.y)
  }

  operator fun plus(other: Delta): Position {
    return Position(this.x + other.x, this.y + other.y)
  }

  fun isSame(other: Position?): Boolean {
    return this === other || (this.x == other?.x && this.y == other?.y)
  }

  fun poPosition(): String {
    return "(${'a' + x},${6 - y})"
  }

  fun boPosition(): String {
    return "(${'A' + x},${6 - y})"
  }

  override fun toString(): String {
    return "(${'a' + x},${6 - y})"
  }
}

fun getIndexFrom(position: Position): Int {
  return position.y * 6 + position.x
}
fun getCoordinatesFrom(index: Int): Position {
  return Position(index % 6, index / 6)
}

data class Board(
  val grid: ByteArray = ByteArray(36) { 0 },
  val gridID: Array<String> = Array<String>(36) { "" },
  val bluePool: List<Byte> = List(8) { PieceType.Po.value },
  val redPool: List<Byte> = List(8) { PieceType.Po.value },
  val numberBlueBo: Int = 0,
  val numberRedBo: Int = 0
) {
  companion object {
    private val ALL_POSITIONS = (0 until 6).flatMap { y ->
      (0 until 6).map { x -> Position(x, y) }
    }
  }

  fun getGridPosition(position: Position): Byte {
    return grid[position.y * 6 + position.x]
  }

  fun getGridIDPosition(position: Position): String {
    return gridID[position.y * 6 + position.x]
  }

  val emptyPositions = ALL_POSITIONS.filter { getGridPosition(it) == 0.toByte() }
  val nonEmptyPositions = ALL_POSITIONS - emptyPositions.toSet()

  // to move into BoardView
  val allPieces: List<Pair<Position, Piece>> = nonEmptyPositions.map {
    it to Piece(getGridIDPosition(it), getGridPosition(it))
  }

  fun getPlayerNumberBo(color: Color): Int {
    return when(color) {
      Color.Blue -> numberBlueBo
      else -> numberRedBo
    }
  }

  fun getPlayerPool(color: Color): List<Byte> {
    return when(color) {
      Color.Blue -> bluePool
      else -> redPool
    }
  }

  fun hasTwoTypesInPool(color: Color): Boolean {
    val pool = getPlayerPool(color)
    return !pool.isEmpty() && pool.first() != pool.last()
  }

  fun removeFromPool(piece: Piece): List<Byte> {
    val pool = getPlayerPool(piece.getColor())
    return when(piece.getType()) {
      PieceType.Po -> pool.subList(0, pool.size - 1) // remove last element
      PieceType.Bo -> pool.subList(1, pool.size) // remove first element
    }
  }

  fun addInPool(piece: Piece): List<Byte> {
    val pool = getPlayerPool(piece.getColor())
    return when(piece.getType()) {
      PieceType.Po -> pool + listOf(PieceType.Po.value) // add as last element
      PieceType.Bo -> listOf(PieceType.Bo.value) + pool // add as first element
    }
  }

  fun isPoolEmpty(player: Color): Boolean = getPlayerPool(player).isEmpty()

  fun getNumberOfBoInPool(player: Color): Int {
    val pool = getPlayerPool(player)
    var count = 0
    for(piece in pool)
      if(piece.toInt() == 2)
        ++count
    return count
  }

  fun getNumberOfPoInPool(player: Color): Int {
    return getPlayerPool(player).size - getNumberOfBoInPool(player)
  }

  fun hasPieceInPool(color: Color, type: PieceType): Boolean {
    val pool = getPlayerPool(color)
    return type.value == when(type) {
      PieceType.Po -> pool.last()
      PieceType.Bo -> pool.first()
    }
  }

  fun hasPieceInPool(piece: Piece): Boolean = hasPieceInPool(piece.getColor(), piece.getType())

  fun pieceAt(position: Position): Piece? {
    if(!isPositionOnTheBoard(position))
      return null

    val value = getGridPosition(position)
    if(value == 0.toByte())
      return null

    return Piece(getGridIDPosition(position), value)
  }

  // push a piece on the board (or outside the board)
  // do nothing if 'from' is invalid
  fun slideFromTo(from: Position, to: Position): Board {
    if(!isPositionOnTheBoard(to)) {
      return removePieceAndPutInPool(from)
    } else {
      val piece = pieceAt(from) ?: return this
      val newGrid = grid.copyOf()
      newGrid[getIndexFrom(from)] = 0
      newGrid[getIndexFrom(to)] = piece.code

      val newGridID = gridID.copyOf()
      newGridID[getIndexFrom(from)] = ""
      newGridID[getIndexFrom(to)] = piece.id

      return Board(
        newGrid,
        newGridID,
        bluePool = this.bluePool,
        redPool = this.redPool,
        numberBlueBo = this.numberBlueBo,
        numberRedBo = this.numberRedBo
      )
    }
  }

  fun removePieceAndPutInPool(position: Position): Board {
    val piece = pieceAt(position) ?: return this

    val newGrid = grid.copyOf()
    newGrid[getIndexFrom(position)] = 0

    val newGridID = gridID.copyOf()
    newGridID[getIndexFrom(position)] = ""

    val newPool = addInPool(piece)
    return when(piece.getColor()) {
      Color.Blue ->
        Board(
          newGrid,
          newGridID,
          bluePool = newPool,
          redPool = this.redPool,
          numberBlueBo = this.numberBlueBo,
          numberRedBo = this.numberRedBo
        )
      Color.Red ->
        Board(
          newGrid,
          newGridID,
          bluePool = this.bluePool,
          redPool = newPool,
          numberBlueBo = this.numberBlueBo,
          numberRedBo = this.numberRedBo
        )
    }
  }

  fun removePieceAndPromoteIt(position: Position): Board {
    val piece = pieceAt(position) ?: return this
    if(piece.getType() == PieceType.Bo) return removePieceAndPutInPool(position)

    val newGrid = grid.copyOf()
    newGrid[getIndexFrom(position)] = 0

    val newGridID = gridID.copyOf()
    newGridID[getIndexFrom(position)] = ""

    val color = piece.getColor()
    val newPool = addInPool(getBoInstanceOfColor(color))

    return when(color) {
      Color.Blue ->
        Board(
          newGrid,
          newGridID,
          bluePool = newPool,
          redPool = this.redPool,
          numberBlueBo = this.numberBlueBo + 1,
          numberRedBo = this.numberRedBo
        )
      Color.Red ->
        Board(
          newGrid,
          newGridID,
          bluePool = this.bluePool,
          redPool = newPool,
          numberBlueBo = this.numberBlueBo,
          numberRedBo = this.numberRedBo + 1
        )
    }
  }

  fun playAt(piece: Piece, at: Position): Board {
    if(!isPositionOnTheBoard(at)) return this

    val newPool = removeFromPool(piece)

    val newGrid = grid.copyOf()
    newGrid[getIndexFrom(at)] = piece.code

    val newGridID = gridID.copyOf()
    newGridID[getIndexFrom(at)] = piece.id

    return when(piece.getColor()) {
      Color.Blue ->
        Board(
          newGrid,
          newGridID,
          bluePool = newPool,
          redPool = this.redPool,
          numberBlueBo = this.numberBlueBo,
          numberRedBo = this.numberRedBo
        )
      Color.Red ->
        Board(
          newGrid,
          newGridID,
          bluePool = this.bluePool,
          redPool = newPool,
          numberBlueBo = this.numberBlueBo,
          numberRedBo = this.numberRedBo
        )
    }
  }

  fun playAt(move: Move): Board = playAt(move.piece, move.to)
}
