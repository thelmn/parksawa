package teamenum.parksawa.data

data class Title(val title: String) : ListItem {
    override val VIEW_TYPE: Int = ViewType.TITLE
}