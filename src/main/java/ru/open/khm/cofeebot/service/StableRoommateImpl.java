package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.entity.Request;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StableRoommateImpl implements StableRoommate {
    @Override
    public Map<Request, Request> getPairs(Map<Request, List<Request>> preferences) {
        checkInput(preferences);
        Pair<Map<Request, Request>, Map<Request, List<Request>>> step1Result = step1(preferences);
        Map<Request, List<Request>> step2Result = step2(step1Result);
        Map<Request, List<Request>> requestListMap = step3(step2Result);
        return requestListMap.entrySet()
                .stream()
                .map(requestListEntry -> Pair.of(requestListEntry.getKey(), requestListEntry.getValue().get(0)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Map<Request, List<Request>> step3(Map<Request, List<Request>> preferences) {
        boolean first = true;

        Pair<List<Request>, List<Request>> table = new ImmutablePair<>(new ArrayList<>(), new ArrayList<>());

        Request firstPreference= null;
        Request secondPreference = null;

        while (true) {
            Request looser = null;
            if ((looser = stableNotPossible(preferences)) != null) {
                throw new StableNotPossibleException(looser);
            }
            for (Request iRequest : preferences.keySet()) {
                if (preferences.get(iRequest).size() >= 2 && first) {
                    firstPreference = iRequest;
                    table.getLeft().add(firstPreference);
                    secondPreference = preferences.get(iRequest).get(1);
                    table.getRight().add(secondPreference);
                    first = false;
                } else if (!first) {
                    if (cycleExists(table)) {
                        preferences = removeCycle(preferences, table);
                        first = true;
                        break;
                    }
                    firstPreference = preferences.get(secondPreference).get(preferences.get(secondPreference).size() - 1);
                    table.getLeft().add(firstPreference);

                    secondPreference = preferences.get(firstPreference).get(1);
                    table.getRight().add(secondPreference);
                }
            }
            if (isStable(preferences)) {
                return preferences;
            }
        }
    }

    private Map<Request, List<Request>> removeCycle(Map<Request, List<Request>> preferences
            , Pair<List<Request>, List<Request>> table) {
        Map<Request, List<Request>> tmpPreferences = preferences;

        for (int i = 0; i < table.getLeft().size() - 1; i++) {
            tmpPreferences.get(table.getRight().get(i)).remove(table.getLeft().get(i + 1));
            tmpPreferences.get(table.getLeft().get(i + 1)).remove(table.getRight().get(i));
        }

        return tmpPreferences;
    }

    private boolean cycleExists(Pair<List<Request>, List<Request>> table) {
        List<Request> left = table.getLeft();
        List<Request> right = table.getRight();
        if (left.size() > right.size()) {
            return true;
        }else {
            return false;
        }
    }

    private boolean isStable(Map<Request, List<Request>> preferences) {
        return preferences.entrySet()
                .stream()
                .allMatch(requestListEntry -> requestListEntry.getValue().size() == 1);
    }

    private Request stableNotPossible(Map<Request, List<Request>> preferences) {
        for (Request request : preferences.keySet()) {
            if (preferences.get(request).size() == 0) {
                return request;
            }
        }
        return null;
    }

    private Map<Request, List<Request>> step2(Pair<Map<Request, Request>
            , Map<Request, List<Request>>> step1Result) {
        Map<Request, List<Request>> inputList = step1Result.getRight();
        Map<Request, Request> proposals = step1Result.getLeft();

        Map<Request, List<Request>> tmpPreferences = clone(inputList);
        for (Request iRequest : tmpPreferences.keySet()) {
            // Remove the right hand side of the preferred element
            int proposalIndex = tmpPreferences.get(iRequest).indexOf(proposals.get(iRequest));
            tmpPreferences.put(iRequest, tmpPreferences.get(iRequest).subList(0, proposalIndex + 1));
            // Remove all other instances of the given element
            for (Request jRequest : inputList.keySet()) {
                //Try to remove element from all preference lists
                Request keyRequest = getKeyByVal(proposals, iRequest);
                if (iRequest != jRequest && jRequest != proposals.get(iRequest) && jRequest != keyRequest) {
                    try {
                        tmpPreferences.get(jRequest).remove(iRequest);
                    } catch (Exception e) {
                        log.warn("Error", e);
                    }
                }
            }
        }

        return tmpPreferences;
    }

    private Request getKeyByVal(Map<Request, Request> input, Request iRequest) {
        for (Map.Entry<Request, Request> requestRequestEntry : input.entrySet()) {
            if (requestRequestEntry.getValue() == iRequest) {
                return requestRequestEntry.getKey();
            }
        }
        return null;
    }

    private Pair<Map<Request, Request>, Map<Request, List<Request>>> step1(Map<Request, List<Request>> preferences) {
        Map<Request, Request> proposals = new HashMap<>();
        Map<Request, Integer> numProposals = new HashMap<>();
        List<Request> queue = new ArrayList<>();

        Map<Request, List<Request>> tmpPreferences = clone(preferences);

        preferences.forEach((request, requests) -> {
            queue.add(request);
            proposals.put(request, null);
            numProposals.put(request, 0);
        });

        while (queue.size() != 0) {
            Request iRequest = queue.get(0);
            numProposals.compute(iRequest, (request1, integer) -> integer + 1);
            if (numProposals.get(iRequest) > proposals.size()) {
                throw new AlgorithmException("A stable matching does not exist.");
            }

            for (Request jRequest : preferences.get(iRequest)) {
                if (proposals.get(jRequest) == null) {
                    queue.remove(0);
                    proposals.put(jRequest, iRequest);
                    break;
                } else if (proposals.get(jRequest) != iRequest) {
                    int currentIndex = preferences.get(jRequest).indexOf(iRequest);
                    int otherIndex = preferences.get(jRequest).indexOf(proposals.get(jRequest));

                    if (currentIndex < otherIndex) {
                        queue.remove(0);
                        queue.add(0, proposals.get(jRequest));
                        tmpPreferences.get(proposals.get(jRequest)).remove(jRequest);
                        tmpPreferences.get(jRequest).remove(proposals.get(jRequest));

                        proposals.put(jRequest, iRequest);
                        break;
                    } else {
                        tmpPreferences.get(iRequest).remove(jRequest);
                        tmpPreferences.get(jRequest).remove(iRequest);
                    }
                }
            }
        }

        return Pair.of(proposals, clone(tmpPreferences));
    }

    private Map<Request, List<Request>> clone(Map<Request, List<Request>> preferences) {
        Map<Request, List<Request>> result = new HashMap<>();
        preferences.forEach((request, requests) -> {
            result.put(request, new ArrayList<>(requests));
        });
        return result;
    }

    private void checkInput(Map<Request, List<Request>> preferences) {
        List<Request> allElements = new ArrayList<>();
        int numElements = preferences.size();
        preferences.forEach((request, requests) -> {
            allElements.add(request);
            if (requests.size() != new HashSet<>(requests).size()) {
                throw new AlgorithmException("Invalid preference list. Duplicate element in a preference list.");
            }
            if (requests.size() != numElements - 1) {
                throw new AlgorithmException("Invalid preference list. Missing element in a preference list.");
            }
        });
        if (allElements.size() > new HashSet<>(allElements).size()) {
            throw new AlgorithmException("Invalid preference list. Duplicate elements exist.");
        }
        allElements.forEach(request -> {
            preferences.forEach((request1, requests) -> {
                if (!request.equals(request1) && !requests.contains(request)) {
                    throw new AlgorithmException("Invalid preference list. Elements don't match in a preference list.");
                }
            });
        });
    }
}
