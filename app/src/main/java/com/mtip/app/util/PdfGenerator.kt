package com.mtip.app.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.mtip.app.data.db.GiftWallet
import com.mtip.app.ui.components.generateQrBitmap
import java.io.File

object PdfGenerator {
    private const val W = 442; private const val H = 188

    fun generate(ctx: Context, w: GiftWallet, qrPayload: String): File {
        val doc = PdfDocument()

        // front
        doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create()).also { p ->
            p.canvas.apply {
                drawColor(Color.WHITE)
                val title = Paint().apply {
                    textSize = 24f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
                }
                val body = Paint().apply {
                    textSize = 10f; color = Color.DKGRAY; isAntiAlias = true
                }
                val small = Paint().apply {
                    textSize = 7f; color = Color.GRAY; isAntiAlias = true
                }
                drawText("ɱTip", 20f, 36f, title)
                drawText("You received a Monero gift!", 20f, 60f, body)
                drawText("Amount: ${Formatters.xmr(w.cachedBalance)}", 20f, 80f, body)
                drawText("Created: ${Formatters.date(w.createdAt)}", 20f, 96f, body)
                drawText("Move funds to your own wallet ASAP.", 20f, 120f, body)
                drawText("Anyone with this card can claim them.", 20f, 136f, body)

                drawText("To claim: install a Monero wallet app, then scan the QR on the back.", 20f, 158f, small)
                drawText("Recommended: Cake Wallet (Google Play) or Monfluo (codeberg.org/acx/monfluo)", 20f, 170f, small)
                drawText("ɱTip app: github.com/PromptPunksFauxCough/mtip", 20f, 182f, small)

                border(this)
            }
            doc.finishPage(p)
        }

        // back
        doc.startPage(PdfDocument.PageInfo.Builder(W, H, 2).create()).also { p ->
            p.canvas.apply {
                drawColor(Color.WHITE)
                val body = Paint().apply {
                    textSize = 8f; color = Color.DKGRAY; isAntiAlias = true
                }
                val mono = Paint().apply {
                    textSize = 6f; color = Color.GRAY; isAntiAlias = true
                    typeface = Typeface.MONOSPACE
                }
                val small = Paint().apply {
                    textSize = 7f; color = Color.GRAY; isAntiAlias = true
                }

                generateQrBitmap(qrPayload, 150)?.let {
                    drawBitmap(it, 10f, 10f, null)
                }

                drawText("Scan QR with ɱTip app to claim", 170f, 20f, body)
                drawText("Or restore with seed in any Monero wallet:", 170f, 34f, body)

                var y = 50f
                w.seed.chunked(42).forEach { line ->
                    drawText(line, 170f, y, mono); y += 12f
                }

                y += 6f
                drawText("Restore height: ${w.restoreHeight}", 170f, y, mono)

                y = 168f
                drawText("Cake Wallet: play.google.com/store/apps/details?id=com.cakewallet.cake_wallet", 10f, y, small)
                y += 10f
                drawText("Monfluo: codeberg.org/acx/monfluo  •  ɱTip: github.com/PromptPunksFauxCough/mtip", 10f, y, small)

                border(this)
            }
            doc.finishPage(p)
        }

        val file = File(ctx.cacheDir, "mtip_gift_${w.id}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun border(c: Canvas) {
        val p = Paint().apply {
            style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 1f
        }
        c.drawRect(2f, 2f, (W - 2).toFloat(), (H - 2).toFloat(), p)
    }
}