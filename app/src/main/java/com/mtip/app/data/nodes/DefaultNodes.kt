package com.mtip.app.data.nodes

data class MoneroNode(val uri: String, val networkType: Int, val isOnion: Boolean = false)

object DefaultNodes {
    val list = listOf(
        // mainnet
        MoneroNode("node.monerodevs.org:18089", 0),
        MoneroNode("node2.monerodevs.org:18089", 0),
        MoneroNode("node3.monerodevs.org:18089", 0),
        MoneroNode("opennode.xmr-tw.org:18089", 0),
        MoneroNode("node.sethforprivacy.com:18089", 0),
        MoneroNode("monero.mullvad.net:18081", 0),
        MoneroNode("plowsof3t5hogddwabaeiyrno25efmzfxyro2vligremt7sxpsclfaid.onion:18089", 0),


        // stagenet
        MoneroNode("node.monerodevs.org:38089", 2),
        MoneroNode("node3.monerodevs.org:38089", 2),
        MoneroNode("node2.monerodevs.org:38089", 2),
        MoneroNode("stagenet.xmr-tw.org:38081", 2),
        MoneroNode("stagenet.xmr.ditatompel.com", 2),
        MoneroNode("ykqlrp7lumcik3ubzz3nfsahkbplfgqshavmgbxb4fauexqzat6idjad.onion:38089", 2),
        MoneroNode("plowsof3t5hogddwabaeiyrno25efmzfxyro2vligremt7sxpsclfaid.onion:38089", 2),
        MoneroNode("plowsoffjexmxalw73tkjmf422gq6575fc7vicuu4javzn2ynnte6tyd.onion:38089", 2),
        MoneroNode("plowsofe6cleftfmk2raiw5h2x66atrik3nja4bfd3zrfa2hdlgworad.onion:38089", 2),

        // testnet
        MoneroNode("node.monerodevs.org:28089", 1),
        MoneroNode("node2.monerodevs.org:28089", 1),
        MoneroNode("testnet.xmr-tw.org:28081", 1),
        MoneroNode("testnet.xmr.ditatompel.com", 1),
        MoneroNode("plowsof3t5hogddwabaeiyrno25efmzfxyro2vligremt7sxpsclfaid.onion:28089", 1),
        MoneroNode("plowsoffjexmxalw73tkjmf422gq6575fc7vicuu4javzn2ynnte6tyd.onion:28089", 1),
        MoneroNode("lgyssws2oary5iclgvcwhwl6c65azwugvskf3iidelo2tzk4wpwblead.onion:28089", 1),
    )

    fun forNetwork(networkType: Int): List<MoneroNode> =
        list.filter { it.networkType == networkType }

    fun defaultForNetwork(networkType: Int): String =
        forNetwork(networkType).firstOrNull()?.uri ?: list.first().uri
}