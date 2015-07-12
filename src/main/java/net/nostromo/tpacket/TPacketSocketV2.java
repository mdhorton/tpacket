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

import net.nostromo.libc.struct.network.tpacket.TPacketReq;

public class TPacketSocketV2 extends TPacketSocket {

    public TPacketSocketV2(final String ifname, final int packetType, final int protocol,
            final int blockSize, final int blockCnt, final int frameSize) {
        super(TPACKET_V2, ifname, packetType, protocol, blockSize, blockCnt, frameSize);
    }

    @Override
    protected void setupRxRing() {
        final TPacketReq tpReq = new TPacketReq();
        tpReq.block_size = blockSize;
        tpReq.block_nr = blockCnt;
        tpReq.frame_size = frameSize;
        tpReq.frame_nr = frameCnt;

        libc.setsockopt(sock, SOL_PACKET, PACKET_RX_RING, tpReq.pointer(), TPacketReq.BYTES);
    }

    // this is currently bugged
    public void setPacketCopyThreshold(final int threshold) {
        util.setPacketCopyThreshold(sock, threshold);
    }
}
