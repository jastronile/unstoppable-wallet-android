package io.horizontalsystems.bankwallet.modules.evmnetwork

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class EvmNetworkService(
    val blockchain: EvmBlockchain,
    private val account: Account,
    private val evmSyncSourceManager: EvmSyncSourceManager
) : Clearable {
    private val disposables = CompositeDisposable()

    private val itemsRelay = BehaviorSubject.create<List<Item>>()
    var items = listOf<Item>()
        private set(value) {
            field = value

            itemsRelay.onNext(value)
        }

    private val currentSyncSource: EvmSyncSource
        get() = evmSyncSourceManager.getSyncSource(account, blockchain)

    init {
        syncItems()
    }

    private fun syncItems() {
        val currentSyncSourceId = currentSyncSource.id

        items = evmSyncSourceManager.getAllBlockchains(blockchain).map { syncSource ->
            Item(syncSource, syncSource.id == currentSyncSourceId)
        }
    }

    val itemsObservable: Observable<List<Item>>
        get() = itemsRelay

    fun setCurrentNetwork(id: String) {
        if (currentSyncSource.id == id) return

        val syncSource = items.find { it.syncSource.id == id }?.syncSource ?: return

        evmSyncSourceManager.save(syncSource, account, blockchain)
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(val syncSource: EvmSyncSource, val selected: Boolean)

}
