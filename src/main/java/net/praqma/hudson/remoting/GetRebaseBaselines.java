/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.remoting;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToListBaselinesException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.utils.BaselineList;
import net.praqma.util.structure.Tuple;

/**
 *
 * Abstraction for polling for new rebase candidates.
 *
 * @author Mads
 */
public class GetRebaseBaselines implements FilePath.FileCallable<Tuple<List<Baseline>, List<Baseline>>> {

    private static final Logger LOG = Logger.getLogger(GetRebaseBaselines.class.getName());
    public final Stream stream;
    public final List<String> components;
    public final Project.PromotionLevel plevel;

    public GetRebaseBaselines(Stream stream, List<String> components, Project.PromotionLevel plevel) {
        this.stream = stream;
        this.components = components;
        this.plevel = plevel;
    }

    /**
     * *
     * Get a private list of baselines for the stream/component combo. Return
     * the newest baseline found the the stream where the baseline originated.
     *
     *
     * @param bl The baseline. We use this {@link Baseline} to get it's {@link Stream}
     * @param plvevel The promotion level to look for
     * @return A {@link BaselineList}
     * @throws UnableToInitializeEntityException Thrown when Cleartool cannot initialise an object
     * @throws UnableToListBaselinesException Thrown on a Cleartool error
     */
    private BaselineList getLatestBaselineFromStream(Baseline bl, Project.PromotionLevel pl) throws UnableToInitializeEntityException, UnableToListBaselinesException {
        BaselineList blsc = new BaselineList();

        if (bl.getStream() != null) {
            blsc = new BaselineList(bl.getStream(), bl.getComponent(), pl).setSorting(new BaselineList.DescendingDateSort()).setLimit(1).apply();
        } else {
            LOG.warning(String.format("The baseline %s did NOT have a stream set", bl));
        }

        return blsc;
    }

    /**
     * Remove the baselines found which we've chosen to ignore.
     *
     * @param bls The {@link List} of {@link Baseline}s
     * @return The pruned list of {@link Baseline}
     */
    private List<Baseline> pruneFromComponent(List<Baseline> bls) {

        Iterator<Baseline> blit = bls.iterator();
        while (blit.hasNext()) {
            Baseline bl = blit.next();
            if (components.contains(bl.getComponent().getNormalizedName())) {
                blit.remove();
            }
        }

        return bls;
    }

    /**
     * Cleanup - We need to to remove the children if there are newer
     *
     * @param The {@link List} of {@link Baseline}s
     * @return The pruned list of {@link Baseline}
     */
    private List<Baseline> pruneFinalListOfBaselines(List<Baseline> bls) {
        ArrayList<Baseline> copy = new ArrayList<>();
        try {
            //Copy the list. We return the pruned list
            copy.addAll(bls);
            for (Baseline bl : bls) {
                Component c = bl.getComponent();
                List<Baseline> dependants = bl.getCompositeDependantBaselines(c);
                copy.removeAll(dependants);
            }

            LOG.finest("We have the following FINAL candidates");
            for (Baseline bl : copy) {
                LOG.finest(String.format(" * %s", bl));
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error in pruneFinalListOfBaselines", e);
        }

        return copy;
    }

    /**
     * @param f A {@link File} usually a workspace denoted by a {@link FilePath}
     * @param channel The channel
     * @return A tuple, with the first value t1 being the baselines found, t2
     * being the proposed new foundation composition.
     * @throws IOException Thrown on system error
     * @throws InterruptedException Thrown on system error
     */
    public Tuple<List<Baseline>, List<Baseline>> invokeDeepStructure(File f, VirtualChannel channel) throws IOException, InterruptedException {
        List<Baseline> foundBaselines = new ArrayList<>();
        Tuple<List<Baseline>, List<Baseline>> result = new Tuple<>();
        HashMap<Component, Baseline> foundationComposition = new HashMap<>();

        /**
         * Break down the Stream whole foundation into a set of components. That
         * is for each component (rootless or rooted) we store 1 baseline.
         */
        try {
            for (Baseline bl : stream.getFoundationBaselines()) {
                foundationComposition.put(bl.getComponent(), bl);

                //Get composite baselines (if any)
                List<Baseline> composites = bl.getCompositeDependantBaselines();

                //Loop through the list of composites. If the baseline found in the composite resides on the top level..Then in all likelyhood 
                //it is an override.                
                for (Baseline blcomp : composites) {
                    //If the foundation contains this particular component override. Then we check that we always have the newer one that overrides
                    //by definition.
                    if (foundationComposition.containsKey(blcomp.getComponent())) {
                        //If the foundation has an override (by default the override MUST be newer)
                        if (blcomp.getDate().after(foundationComposition.get(blcomp.getComponent()).getDate())) {
                            foundationComposition.put(blcomp.getComponent(), blcomp);
                        }
                    } else {
                        foundationComposition.put(blcomp.getComponent(), blcomp);
                    }
                }
            }

            //For each component see if there is a newer baseline. The list of baselines we get is a sorted set, that takes the newest as the first element,
            //and we only return the latest one. If the latest one is newer than the current, it must be different.             
            for (Map.Entry<Component, Baseline> entry : foundationComposition.entrySet()) {

                BaselineList blsc = getLatestBaselineFromStream(entry.getValue(), plevel);
                if (!blsc.isEmpty() && blsc.get(0).getDate().after(entry.getValue().getDate())) {
                    //Only add if newer
                    foundBaselines.addAll(blsc);
                }
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error caught in invokeDeepStructure", ex);
        }

        //Filter dependant baselines (That is there was a newer dependency baseline, we need not include the children.) 
        foundBaselines = pruneFinalListOfBaselines(foundBaselines);

        //Filter away ignored components
        foundBaselines = pruneFromComponent(foundBaselines);

        //The new baselines should override the old ones. Drop everything else on the foundation.
        for (Baseline bl : foundBaselines) {
            foundationComposition.put(bl.getComponent(), bl);
        }

        //Make sure that the newest baseline is the one we use. We rebase to all baselines found anyway so this essential makes sure that the
        //'HEAD' baseline is the one we see in the Gui. 
        Collections.sort(foundBaselines, new BaselineList.DescendingDateSort());

        result.t1 = foundBaselines;
        result.t2 = new ArrayList<>(foundationComposition.values());

        //Return the result - 
        return result;
    }

    /**
     * The simple one. This poll top level structure, that means that we'll
     * never add an override entry. Thus..we can just use the defined
     * foundations to get the baselines we need.
     *
     * @param f File
     * @param channel Channel
     * @return A {@link Tuple} containing the foundation {@link Baseline}s and Components
     * @throws IOException An exception thrown on system error
     * @throws InterruptedException An exception thrown on system error
     */
    private Tuple<List<Baseline>, List<Baseline>> invokeUsingFlatStructure(File f, VirtualChannel channel) throws IOException, InterruptedException {

        List<Baseline> foundBaselines = new ArrayList<>();
        Tuple<List<Baseline>, List<Baseline>> result = new Tuple<>();
        
        HashMap<Component,Baseline> composition = new HashMap<>();
        
        //Extract foundation baselines
        List<Baseline> foundations = stream.getFoundationBaselines();

        try {
            for (Baseline bl : foundations) {
                //Record the composition of the foundation
                composition.put(bl.getComponent(), bl);
                
                //Look for a newer baseline
                BaselineList bls = getLatestBaselineFromStream(bl, plevel);
                
                //If there was a newer baseline. 
                if(!bls.isEmpty()) {
                    LOG.log(Level.FINEST, String.format("Comparing %s to foundation %s", bls.get(0), bl));
                }
                
                if (!bls.isEmpty() && !components.contains(bl.getComponent().getNormalizedName()) && bls.get(0).getDate().after(bl.getDate()) && !stream.equals(bls.get(0).getStream()) ) {
                    //Do not include baselines created on this stream
                    composition.put(bl.getComponent(), bls.get(0));
                    foundBaselines.addAll(bls);                    
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error caught", ex);
        }

        //Make sure that the newest baseline is the one we use. We rebase to all baselines found anyway so this essential makes sure that the
        //'HEAD' baseline is the one we see in the Gui. 
        Collections.sort(foundBaselines, new BaselineList.DescendingDateSort());

        result.t1 = foundBaselines;
        result.t2 = new ArrayList<>(composition.values());
        
        return result;
    }

    @Override
    public Tuple<List<Baseline>, List<Baseline>> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        LOG.finest("Invoking GetRebaseBaselines");
        return invokeUsingFlatStructure(f, channel);
    }

}
