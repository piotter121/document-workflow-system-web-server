package pl.edu.pw.ee.pyskp.documentworkflow.services.impl;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.springframework.stereotype.Service;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.Difference;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.DifferenceType;
import pl.edu.pw.ee.pyskp.documentworkflow.services.DifferenceService;
import pl.edu.pw.ee.pyskp.documentworkflow.utils.TikaUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by piotr on 06.01.17.
 */
@Service
public class DifferenceServiceImpl implements DifferenceService {

    private final TikaUtils tikaUtils = new TikaUtils();

    private Difference mapDeltaToDifference(Delta<String> delta) {
        Chunk<String> original = delta.getOriginal();
        Chunk<String> revised = delta.getRevised();
        Difference difference = new Difference();
        difference.setPreviousSectionStart((long) original.getPosition());
        difference.setPreviousSectionSize((long) original.size());
        difference.setNewSectionStart((long) revised.getPosition());
        difference.setNewSectionSize((long) revised.size());
        DifferenceType differenceType = DifferenceType.fromDeltaType(delta.getType())
                .orElseThrow(() -> new IllegalArgumentException("Delta type is null"));
        difference.setDifferenceType(differenceType);
        return difference;
    }

    @Override
    public List<Difference> createDifferencesForNewFile(InputStream inputStream) throws IOException {
        List<String> lines = tikaUtils.extractLines(inputStream);
        Patch<String> diff = DiffUtils.diff(Collections.emptyList(), lines);
        return diff.getDeltas().stream()
                .map(this::mapDeltaToDifference)
                .collect(Collectors.toList());
    }

    @Override
    public List<Difference> getDifferencesBetweenTwoFiles(InputStream inputStream, InputStream anotherInputStream)
            throws IOException {
        Patch<String> diff =
                DiffUtils.diff(tikaUtils.extractLines(inputStream), tikaUtils.extractLines(anotherInputStream));
        return diff.getDeltas().stream()
                .map(this::mapDeltaToDifference)
                .collect(Collectors.toList());
    }
}