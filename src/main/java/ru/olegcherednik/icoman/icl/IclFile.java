package ru.olegcherednik.icoman.icl;

import ru.olegcherednik.icoman.AbstractIconFile;
import ru.olegcherednik.icoman.IconIO;
import ru.olegcherednik.icoman.ImageKey;
import ru.olegcherednik.icoman.exceptions.FormatNotSupportedException;
import ru.olegcherednik.icoman.exceptions.IconManagerException;
import ru.olegcherednik.icoman.exceptions.ImageNotFoundException;
import ru.olegcherednik.icoman.icl.imageio.IclReaderSpi;

import javax.imageio.stream.ImageInputStream;
import javax.validation.constraints.NotNull;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Oleg Cherednik
 * @since 02.10.2016
 */
public final class IclFile extends AbstractIconFile {
    private final Map<String, Map<String, Image>> icoByName;

    public IclFile(ImageInputStream in) throws Exception {
        this(read(in));
    }

    private IclFile(Map<String, Map<String, Image>> images) {
        super(createImageById(images));
        icoByName = createIcoByName(images);
    }

    @NotNull
    public Set<String> getNames() {
        return icoByName.isEmpty() ? Collections.emptySet() : icoByName.keySet();
    }

    @NotNull
    public Map<String, Image> getImages(String name) throws ImageNotFoundException {
        if (!icoByName.containsKey(name))
            throw new ImageNotFoundException(name);
        return icoByName.get(name);
    }

    // ========== static ==========

    private static Map<String, Map<String, Image>> read(ImageInputStream in) throws Exception {
        checkMarkZbikowskiSignature(in);
        skipDosHeader(in);
        int ntHeaderOffs = in.readUnsignedShort();
        in.seek(ntHeaderOffs);
        NtHeader ntHeader = new NtHeader(in);
        OptionalHeader optionalHeader = ntHeader.getOptionalHeader();
        Map<String, SectionHeader> sectionHeaders = readSectionTable(in, ntHeader.getFileHeader().getNumberOfSection());
        long offsZero = rvaToOffs(sectionHeaders.values(), DataDirectory.Entry.RESOURCE.getRva(optionalHeader), optionalHeader.getSectionAlignment());
        in.seek(offsZero);
        return readIconsResources(in, offsZero);
    }

    private static Map<String, Map<String, Image>> createIcoByName(Map<String, Map<String, Image>> imagesByNameId) {
        Map<String, Map<String, Image>> idByName = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Image>> entry : imagesByNameId.entrySet())
            idByName.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));

        return Collections.unmodifiableMap(idByName);
    }

    private static Map<String, Image> createImageById(Map<String, Map<String, Image>> imagesByNameId) {
        Map<String, Image> imageById = new LinkedHashMap<>();
        imagesByNameId.values().forEach(imageById::putAll);
        return Collections.unmodifiableMap(imageById);
    }

    private static void skipNamedEntries(ImageInputStream in, int total) throws IOException {
        in.skipBytes(total * ResourceDirectoryEntry.SIZE);
    }

    private static void skipDosHeader(ImageInputStream in) throws IOException {
        in.skipBytes(58);
    }

    private static Map<Integer, ResourceDirectoryEntry> readResourceDirectoryEntries(ImageInputStream in, int total, boolean idDec)
            throws IOException {
        ResourceDirectoryEntry entry;
        Map<Integer, ResourceDirectoryEntry> entries = new HashMap<>();

        for (int i = 0; i < total; i++)
            entries.put((entry = new ResourceDirectoryEntry(in, idDec)).getId(), entry);

        return entries;
    }

    private static Map<String, Map<String, Image>> readIconsResources(ImageInputStream in, long offsZero) throws IOException, IconManagerException {
        ResourceDirectory resourceDirectory = ResourceDirectory.read(in);
        skipNamedEntries(in, resourceDirectory.getNumberOfNamedEntries());
        Map<Integer, ResourceDirectoryEntry> resourceDirectoryEntries =
                readResourceDirectoryEntries(in, resourceDirectory.getNumberOfIdEntries(), false);

        ResourceDirectoryEntry entryGroupIconName = resourceDirectoryEntries.get(45);
        ResourceDirectoryEntry entryGroupIcon = resourceDirectoryEntries.get(14);
        ResourceDirectoryEntry entryIcon = resourceDirectoryEntries.get(3);

        List<String> groupIconNames = readGroupIconName(in, offsZero, entryGroupIconName);
        Map<String, Set<ImageHeader>> groupIcons = readGroupIcon(in, offsZero, entryGroupIcon, groupIconNames);
        Map<Integer, Image> icons = readIcon(in, offsZero, entryIcon);

        return getImageByIdName(groupIcons, icons);
    }

    private static Map<String, Map<String, Image>> getImageByIdName(Map<String, Set<ImageHeader>> groupIcons, Map<Integer, Image> icons) {
        Map<String, Map<String, Image>> imageByIdName = new LinkedHashMap<>();

        for (Map.Entry<String, Set<ImageHeader>> entry : groupIcons.entrySet()) {
            String name = entry.getKey();
            Map<String, Image> imageById = new LinkedHashMap<>();

            entry.getValue().stream()
                 .filter(imageHeader -> icons.containsKey(imageHeader.getPos()))
                 .forEach(imageHeader -> {
                     String id = ImageKey.parse(name, imageHeader.getWidth(), imageHeader.getHeight(), imageHeader.getBitsPerPixel());
                     imageById.put(id, icons.get(imageHeader.getPos()));
                 });

            imageByIdName.put(name, imageById);
        }

        return imageByIdName;
    }

    private static long getLeafOffs(ImageInputStream in, long offsZero, long offs) throws IOException, IconManagerException {
        in.seek(offsZero + offs);
        ResourceDirectory resourceDirectory = ResourceDirectory.read(in);

        if (resourceDirectory.getNumberOfNamedEntries() != 0)
            throw new IconManagerException();
        if (resourceDirectory.getNumberOfIdEntries() != 1)
            throw new IconManagerException();

        return getLeafOffs(new ResourceDirectoryEntry(in, false), in, offsZero) - offsZero;
    }

    private static long getLeafOffs(ResourceDirectoryEntry entry, ImageInputStream in, long offsZero)
            throws IOException, IconManagerException {
        return offsZero + (entry.isLeaf() ? entry.getOffsData() : getLeafOffs(in, offsZero, entry.getOffsData()));
    }

    private static List<String> readGroupIconName(ImageInputStream in, long offsZero, ResourceDirectoryEntry entryGroupIconName)
            throws IOException, IconManagerException {
        if (entryGroupIconName == null)
            return Collections.emptyList();

        in.seek(getLeafOffs(entryGroupIconName, in, offsZero));
        in.seek(ResourceDataEntry.read(in).getRva());

        checkIclSignature(in);

        int length;
        List<String> names = new ArrayList<>();

        while ((length = in.readUnsignedByte()) != 0) {
            names.add(IconIO.readString(in, length));
        }

        return names;
    }

    private static Map<String, Set<ImageHeader>> readGroupIcon(ImageInputStream in, long offsZero, ResourceDirectoryEntry entryGroupIcon,
            List<String> groupIconNames) throws IOException, IconManagerException {
        if (entryGroupIcon == null || groupIconNames.isEmpty())
            return Collections.emptyMap();
        if (entryGroupIcon.isLeaf())
            throw new IconManagerException();

        in.seek(offsZero + entryGroupIcon.getOffsData());
        ResourceDirectory resourceDirectory = ResourceDirectory.read(in);

        if (resourceDirectory.getNumberOfNamedEntries() != 0)
            throw new IconManagerException();
        if (resourceDirectory.getNumberOfIdEntries() != groupIconNames.size())
            throw new IconManagerException();

        Map<Integer, ResourceDirectoryEntry> entries = readResourceDirectoryEntries(in, resourceDirectory.getNumberOfIdEntries(), true);
        Map<String, Set<ImageHeader>> map = new LinkedHashMap<>();
        int pos = 0;

        for (Map.Entry<Integer, ResourceDirectoryEntry> entry : entries.entrySet()) {
            in.seek(getLeafOffs(entry.getValue(), in, offsZero));
            ResourceDataEntry resourceDataEntry = ResourceDataEntry.read(in);
            in.seek(resourceDataEntry.getRva());

            Set<ImageHeader> imageHeaders = new TreeSet<>();

            for (int i = 0, total = resourceDataEntry.getSize() / ImageHeader.SIZE; i < total; i++, pos++)
                imageHeaders.add(ImageHeader.read(pos, in));

            map.put(groupIconNames.get(entry.getKey()), imageHeaders);
        }

        return map;
    }

    private static Map<Integer, Image> readIcon(ImageInputStream in, long offsZero, ResourceDirectoryEntry entryIcon)
            throws IconManagerException, IOException {
        if (entryIcon == null)
            return Collections.emptyMap();
        if (entryIcon.isLeaf())
            throw new IconManagerException();

        in.seek(offsZero + entryIcon.getOffsData());
        ResourceDirectory resourceDirectory = ResourceDirectory.read(in);

        if (resourceDirectory.getNumberOfNamedEntries() != 0)
            throw new IconManagerException();

        Map<Integer, ResourceDirectoryEntry> entries = readResourceDirectoryEntries(in, resourceDirectory.getNumberOfIdEntries(), true);
        Map<Integer, Image> map = new LinkedHashMap<>();

        for (Map.Entry<Integer, ResourceDirectoryEntry> entry : entries.entrySet()) {
            in.seek(getLeafOffs(entry.getValue(), in, offsZero));
            ResourceDataEntry resourceDataEntry = ResourceDataEntry.read(in);
            in.seek(resourceDataEntry.getRva());
            map.put(entry.getKey(), IconIO.readImage(in, resourceDataEntry.getSize()));
        }

        return map;
    }

    private static void checkIclSignature(ImageInputStream in) throws IOException, IconManagerException {
        if (!"ICL".equals(IconIO.readString(in, in.readUnsignedByte())))
            throw new IconManagerException();
    }

    private static Map<String, SectionHeader> readSectionTable(ImageInputStream in, int numberOfSection) throws IOException {
        if (numberOfSection <= 0)
            return Collections.emptyMap();

        SectionHeader header;
        Map<String, SectionHeader> sectionHeaders = new LinkedHashMap<>(numberOfSection);

        for (int i = 0; i < numberOfSection; i++)
            sectionHeaders.put((header = new SectionHeader(in)).getName(), header);

        return sectionHeaders;
    }

    private static void checkMarkZbikowskiSignature(ImageInputStream in) throws IOException, FormatNotSupportedException {
        if (!IclReaderSpi.isHeaderValid(in.readUnsignedShort()))
            throw new FormatNotSupportedException("Expected MZ format: 'rva:0, size:2' should be 'MZ'");
    }

    private static long allignDown(long x, long align) {
        return x & ~(align - 1);
    }

    private static long allignUp(long x, long align) {
        return (x & (align - 1)) != 0 ? allignDown(x, align) + align : x;
    }

    private static SectionHeader defSection(Collection<SectionHeader> sectionHeaders, long rva, long sectionAlignment) {
        for (SectionHeader sectionHeader : sectionHeaders) {
            long start = sectionHeader.getVirtualAddress();
            long end = start + allignUp(sectionHeader.getMisc(), sectionAlignment);

            if (rva >= start && rva < end)
                return sectionHeader;
        }

        return null;
    }

    private static long rvaToOffs(Collection<SectionHeader> sectionHeaders, long rva, long sectionAlignment) {
        SectionHeader sectionHeader = defSection(sectionHeaders, rva, sectionAlignment);
        return sectionHeader != null ? rva - sectionHeader.getVirtualAddress() + sectionHeader.getPointerToRawData() : 0;
    }
}
