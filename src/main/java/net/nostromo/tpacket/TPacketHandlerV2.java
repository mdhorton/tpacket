/*
 * Copyright (c) 2015 Mark D. Horton
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABIL-
 * ITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.nostromo.tpacket;

import net.nostromo.libc.struct.io.PollFd;
import net.nostromo.libc.struct.network.tpacket.header.TPacket2Hdr;

public class TPacketHandlerV2 extends TPacketHandler {

    protected final TPacket2Hdr tp2Hdr;

    public TPacketHandlerV2(final TPacketSocket socket) {
        super(socket);
        tp2Hdr = new TPacket2Hdr(buffer);
    }

    @Override
    public void loop() {
        final PollFd pollfd = new PollFd();
        pollfd.fd = sock.sock;
        pollfd.events = (short) POLLIN;

        int index = 0;

        while (true) {
            final long startOffset = (long) sock.frameSize * index;
            final long statusOffset = sock.mmap + startOffset;

            poll(pollfd, startOffset, statusOffset);

            index++;
            if (index == sock.frameCnt) index = 0;
        }
    }

    @Override
    protected void handleFrame(final long offset) {
        tp2Hdr.read(offset);
        handleEthernetPacket(offset + tp2Hdr.mac, tp2Hdr.snaplen);
    }
}
