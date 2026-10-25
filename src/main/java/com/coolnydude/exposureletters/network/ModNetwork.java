package com.coolnydude.exposureletters.network;

import com.coolnydude.exposureletters.client.ClientHooks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(Payloads.EditNote.TYPE, Payloads.EditNote.CODEC, ServerHandlers::editNote);
        r.playToServer(Payloads.TakeFromLectern.TYPE, Payloads.TakeFromLectern.CODEC, ServerHandlers::takeFromLectern);
        r.playToServer(Payloads.RequestLecternSync.TYPE, Payloads.RequestLecternSync.CODEC, ServerHandlers::requestLecternSync);
        r.playToServer(Payloads.UpdateEnvelope.TYPE, Payloads.UpdateEnvelope.CODEC, ServerHandlers::updateEnvelope);
        r.playToServer(Payloads.SaveNewspaper.TYPE, Payloads.SaveNewspaper.CODEC, ServerHandlers::saveNewspaper);
        r.playToClient(Payloads.OpenLecternNote.TYPE, Payloads.OpenLecternNote.CODEC,
                (p, c) -> ClientHooks.openNoteOnLectern(p.pos(), p.content()));
        r.playToClient(Payloads.OpenLecternNewspaper.TYPE, Payloads.OpenLecternNewspaper.CODEC,
                (p, c) -> ClientHooks.openNewspaperReader(p.content(), p.pos()));
        r.playToClient(Payloads.SyncLectern.TYPE, Payloads.SyncLectern.CODEC,
                (p, c) -> ClientHooks.syncLectern(p.pos(), p.book()));
    }

    private ModNetwork() {}
}
