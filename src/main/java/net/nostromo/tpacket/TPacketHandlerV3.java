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
import net.nostromo.libc.struct.network.tpacket.block.TPacketBdHeaderU;
import net.nostromo.libc.struct.network.tpacket.block.TPacketBlockDesc;
import net.nostromo.libc.struct.network.tpacket.header.TPacket3Hdr;
import net.nostromo.libc.struct.network.tpacket.header.TPacketHdrVariant1Union;

public class TPacketHandlerV3 extends TPacketHandler {

    protected final TPacketBlockDesc blockHdr;
    protected final TPacket3Hdr tp3Hdr;

    public TPacketHandlerV3(final TPacketSocket socket) {
        super(socket);
        blockHdr = new TPacketBlockDesc(buffer, TPacketBdHeaderU.Name.BH1);
        tp3Hdr = new TPacket3Hdr(buffer, TPacketHdrVariant1Union.Name.HV1);
    }

    @Override
    public void loop() {
        final PollFd pollfd = new PollFd();
        pollfd.fd = sock.sock;
        pollfd.events = (short) POLLIN;

        int index = 0;

        while (true) {
            final long startOffset = (long) sock.blockSize * index;
            final long statusOffset = sock.mmap + startOffset + 8L;

            poll(pollfd, startOffset, statusOffset);

            index++;
            if (index == sock.blockCnt) index = 0;
        }
    }

    @Override
    protected void handleFrame(final long offset) {
        blockHdr.read(offset);
        long tp3HdrOffset = offset + blockHdr.hdr_u.bh1.offset_to_first_pkt;

        for (int idx = 0; idx < blockHdr.hdr_u.bh1.num_pkts; idx++) {
            tp3Hdr.read(tp3HdrOffset);
            handleEthernetPacket(tp3HdrOffset + tp3Hdr.mac, tp3Hdr.snaplen);
            tp3HdrOffset += tp3Hdr.next_offset;
        }
    }
}
