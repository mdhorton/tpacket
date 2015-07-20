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

import net.nostromo.libc.Libc;
import net.nostromo.libc.LibcConstants;
import net.nostromo.libc.LibcUtil;
import net.nostromo.libc.struct.IntRef;
import net.nostromo.libc.struct.network.socket.SockAddrLl;

import static net.nostromo.libc.Util.htonl;

public abstract class TPacketSocket implements LibcConstants {

    protected static final Libc libc = Libc.libc;
    protected static final LibcUtil util = LibcUtil.util;

    protected final int version;
    protected final String ifname;
    protected final int packetType;
    protected final int protocol;
    protected final int blockSize;
    protected final int blockCnt;
    protected final int frameSize;
    protected final long mapSize;
    protected final int frameCnt;

    protected int sock;
    protected long mmap;

    public TPacketSocket(final int version, final String ifname, final int packetType,
            final int protocol, final int blockSize, final int blockCnt, final int frameSize) {
        this.version = version;
        this.ifname = ifname;
        this.packetType = packetType;
        this.protocol = protocol;
        this.blockSize = blockSize;
        this.blockCnt = blockCnt;
        this.frameSize = frameSize;
        mapSize = (long) blockSize * blockCnt;
        frameCnt = (int) (mapSize / frameSize);
    }

    public void initialize() {
        sock = createSocket();
        setupVersion();
        setupRxRing();
        mmap = createMmap();
        bindSocket();
    }

    protected abstract void setupRxRing();

    protected int createSocket() {
        return libc.socket(PF_PACKET, packetType, htonl(protocol));
    }

    protected void setupVersion() {
        final IntRef ref = new IntRef(version);
        libc.setsockopt(sock, SOL_PACKET, PACKET_VERSION, ref.pointer(), Integer.BYTES);
    }

    protected long createMmap() {
        return libc.mmap(0, mapSize, PROT_READ | PROT_WRITE, MAP_SHARED, sock, 0);
    }

    protected void bindSocket() {
        final SockAddrLl sall = new SockAddrLl();
        sall.family = PF_PACKET;
        sall.protocol = (short) protocol;
        sall.ifindex = util.getInterfaceId(sock, ifname);

        libc.bind(sock, sall.pointer(), SockAddrLl.BYTES);
    }

    public void setupFanout(final int fanoutId, final int fanoutType) {
        final IntRef ref = new IntRef((fanoutId & 0xffff) | (fanoutType << 16));
        libc.setsockopt(sock, SOL_PACKET, PACKET_FANOUT, ref.pointer(), Integer.BYTES);
    }

    public void enablePromiscuous() {
        util.enablePromiscMode(sock, ifname);
    }

    public void attachFilter(final Object[][] filterObjs) {
        util.attachFilter(sock, filterObjs);
    }

    public int getSock() {
        return sock;
    }

    public long getMmap() {
        return mmap;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public int getFrameCnt() {
        return frameCnt;
    }
}
