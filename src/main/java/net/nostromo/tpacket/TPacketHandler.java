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
import net.nostromo.libc.OffHeapBuffer;
import net.nostromo.libc.TheUnsafe;
import net.nostromo.libc.struct.io.PollFd;
import net.nostromo.libc.struct.network.header.EthHdr;
import net.nostromo.libc.struct.network.header.IpHdr;
import net.nostromo.libc.struct.network.header.TcpHdr;
import sun.misc.Unsafe;

public abstract class TPacketHandler implements LibcConstants {

    protected static final Libc libc = Libc.libc;
    protected static final Unsafe unsafe = TheUnsafe.unsafe;

    protected final TPacketSocket sock;
    protected final byte[] payload;

    protected final OffHeapBuffer buffer;
    protected final EthHdr ethHdr;
    protected final IpHdr ipHdr;
    protected final TcpHdr tcpHdr;

    protected long totPackets;
    protected long totBytes;

    public TPacketHandler(final TPacketSocket sock) {
        this(sock, 1 << 16); // 65k
    }

    public TPacketHandler(final TPacketSocket sock, final int payloadSize) {
        this.sock = sock;
        buffer = OffHeapBuffer.attach(sock.mmap);
        ethHdr = new EthHdr(buffer);
        ipHdr = new IpHdr(buffer);
        tcpHdr = new TcpHdr(buffer);
        payload = new byte[payloadSize];
    }

    public abstract void loop();

    protected abstract void handleFrame(long offset);

    protected void poll(final PollFd pollfd, final long startOffset, final long statusOffset) {
        if ((unsafe.getInt(statusOffset) & TP_STATUS_USER) == 0) {
            pollfd.revents = 0;
            libc.poll(pollfd.pointer(), 1, -1);
            pollfd.read();

            if ((pollfd.revents & POLLIN) == 0) {
                throw new RuntimeException("libc.poll() error");
            }
        }

        handleFrame(startOffset);
        unsafe.putInt(statusOffset, TP_STATUS_KERNEL);
        unsafe.fullFence();
    }

    protected void handleEthernetPacket(final long linkLayerOffset, final int snaplen) {
        totPackets++;
        totBytes += snaplen;

        if (snaplen < EthHdr.BYTES) return;

        ethHdr.read(buffer, linkLayerOffset);

        final long inetLayerOffset = linkLayerOffset + EthHdr.BYTES;
        final int remainder = snaplen - EthHdr.BYTES;

        switch (Short.toUnsignedInt(ethHdr.eth_type)) {
            case ETH_P_IP:
                handleIpV4Packet(inetLayerOffset, remainder);
                break;
            case ETH_P_IPV6:
                handleIpV6Packet(inetLayerOffset, remainder);
                break;
            case ETH_P_ARP:
                handleArpPacket(inetLayerOffset, remainder);
                break;
            case ETH_P_RARP:
                handleRarpPacket(inetLayerOffset, remainder);
                break;
            default:
                handleUnknownEtherType(inetLayerOffset, remainder);
        }
    }

    protected void handleIpV4Packet(final long offset, final int snaplen) {
        if (snaplen < IpHdr.BYTES) return;

        ipHdr.read(buffer, offset);

        final long protoOffset = offset + ipHdr.hdr_len_bytes;
        final int remainder = snaplen - IpHdr.BYTES;

        switch (ipHdr.protocol) {
            case IPPROTO_TCP:
                handleTcpPacket(protoOffset, remainder);
                break;
            case IPPROTO_ICMP:
                handleIcmpPacket(protoOffset, remainder);
                break;
            default:
                handleUnknownIpType(protoOffset, remainder);
        }
    }

    protected void handleIpV6Packet(final long offset, final int snaplen) { }

    protected void handleArpPacket(final long offset, final int snaplen) { }

    protected void handleRarpPacket(final long offset, final int snaplen) { }

    protected void handleTcpPacket(final long offset, final int snaplen) {
        if (snaplen < TcpHdr.BYTES) return;

        tcpHdr.read(buffer, offset);
        ipV4Payload(snaplen);
    }

    protected void handleIcmpPacket(final long offset, final int snaplen) {
        ipV4Payload(snaplen);
    }

    protected void handleUnknownEtherType(final long offset, final int snaplen) {}

    protected void handleUnknownIpType(final long offset, final int snaplen) { }

    protected void ipV4Payload(final int snaplen) {
        final int payloadLen = ipHdr.tot_len - ipHdr.hdr_len_bytes;
        final int truncated = Math.min(snaplen, payloadLen);

        buffer.getBytes(payload, truncated);
    }
}
