package com.github.tidetune.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.koin.compose.viewmodel.koinViewModel
import com.github.tidetune.core.DataSourceKeyH
import com.github.tidetune.viewmodels.AssetVM
import uniffi.tidetune_core.DataSourceKey

@Composable
fun TideTuneImage(
    modifier: Modifier = Modifier,
    dataSourceKey: DataSourceKey,
    contentScale: ContentScale,
    vm: AssetVM = koinViewModel()
) {
    var oldKey: DataSourceKeyH by remember { mutableStateOf(DataSourceKeyH(dataSourceKey)) }
    var bitmap: ImageBitmap? by remember { mutableStateOf(vm.getBitmap(dataSourceKey)) }
    val key = DataSourceKeyH(dataSourceKey)

    LaunchedEffect(key.hashCode(), bitmap != null) {
        if (key != oldKey || bitmap == null) {
            oldKey = key
            bitmap = vm.loadBitmap(key.value())
        }
    }


    if (bitmap == null) {
        return
    }

    Image(
        modifier = modifier,
        bitmap = bitmap!!,
        contentDescription = null,
        contentScale = contentScale,
    )
}
