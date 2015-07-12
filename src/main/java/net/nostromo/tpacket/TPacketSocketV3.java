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

import net.nostromo.libc.struct.network.tpacket.TPacketReq3;

public class TPacketSocketV3 extends TPacketSocket {

    protected final int timeout;
    protected final int privOffset;
    protected final int featureReq;

    public TPacketSocketV3(final String ifname, final int packetType, final int protocol,
            final int blockSize, final int blockCnt, final int frameSize, final int timeout) {
        this(ifname, packetType, protocol, blockSize, blockCnt, frameSize, timeout, 0, 0);
    }

    public TPacketSocketV3(final String ifname, final int packetType, final int protocol,
            final int blockSize, final int blockCnt, final int frameSize, final int timeout,
            final int featureReq) {
        this(ifname, packetType, protocol, blockSize, blockCnt, frameSize, timeout, 0, featureReq);
    }

    public TPacketSocketV3(final String ifname, final int packetType, final int protocol,
            final int blockSize, final int blockCnt, final int frameSize, final int timeout,
            final int privOffset, final int featureReq) {
        super(TPACKET_V3, ifname, packetType, protocol, blockSize, blockCnt, frameSize);
        this.timeout = timeout;
        this.privOffset = privOffset;
        this.featureReq = featureReq;
    }

    @Override
    protected void setupRxRing() {
        final TPacketReq3 tpReq3 = new TPacketReq3();
        tpReq3.block_size = blockSize;
        tpReq3.block_nr = blockCnt;
        tpReq3.frame_size = frameSize;
        tpReq3.frame_nr = frameCnt;
        tpReq3.retire_blk_tov = timeout;
        tpReq3.sizeof_priv = privOffset;
        tpReq3.feature_req_word = featureReq;

        libc.setsockopt(sock, SOL_PACKET, PACKET_RX_RING, tpReq3.pointer(), TPacketReq3.BYTES);
    }
}
