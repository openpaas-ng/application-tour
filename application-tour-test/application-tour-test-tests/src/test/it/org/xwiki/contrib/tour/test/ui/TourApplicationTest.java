/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.tour.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.tour.test.po.PageWithTour;
import org.xwiki.contrib.tour.test.po.StepEditModal;
import org.xwiki.contrib.tour.test.po.TourEditPage;
import org.xwiki.contrib.tour.test.po.TourFromLivetable;
import org.xwiki.contrib.tour.test.po.TourHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id: $
 * @since 0.2
 */
public class TourApplicationTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule =
        new SuperAdminAuthenticationRule(getUtil(), getDriver());

    private void setUpTour(TourEditPage tourEditPage, String description, boolean isActive, String targetPage,
        String targetClass) throws Exception
    {
        tourEditPage.setDescription(description);
        tourEditPage.setIsActive(isActive);
        tourEditPage.setTargetPage(targetPage);
        tourEditPage.setTargetClass(targetClass);
    }

    private void setUpStep(TourEditPage tourEditPage, String element, String title, String content,
            boolean backdrop, String targetPage)
    {
        StepEditModal stepEditModal = tourEditPage.newStep();
        stepEditModal.setElement(element);
        stepEditModal.setTitle(title);
        stepEditModal.setContent(content);
        stepEditModal.setBackdrop(backdrop);
        stepEditModal.setTargetPage(targetPage);
        stepEditModal.save();
    }


    private void cleanUp(String page) throws Exception
    {
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        ViewPage tourPage = tourHomePage.getTourPage(page);
        tourPage.delete().clickYes();
    }

    @Test
    public void testTour() throws Exception
    {
        // First, we need to create a tour
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        TourEditPage tourEditPage = tourHomePage.addNewEntry("Test");
        setUpTour(tourEditPage, "My nice description", true, "Tour.WebHome", "");

        // Test to put a translation key, use the translation macro
        setUpStep(tourEditPage, "body", "tour.app.name", "{{translation key=\"TourCode.TourClass_description\" /}}",
                true, "");
        // I voluntary create the object 3 before the 2 to test the 'order' field
        setUpStep(tourEditPage, "body", "Title 3", "Step 3", true, "");
        setUpStep(tourEditPage, "body", "Title 2", "Step 2", true, "");
        // Add a step that will be removed
        setUpStep(tourEditPage, "body", "to remove", "to remove", false, "");
        // Object 4 used to test the Multipage feature ('targetPage' field)
        setUpStep(tourEditPage, "body", "Title 4", "Step 4", true, "TourCode.TourClass");


        // Test that we can change the order of a step
        StepEditModal stepEditModal = tourEditPage.editStep(2);
        assertEquals("body", stepEditModal.getElement());
        assertEquals("Title 3", stepEditModal.getTitle());
        assertEquals("Step 3", stepEditModal.getContent());
        assertEquals(2, stepEditModal.getOrder());
        assertTrue(stepEditModal.isBackdropEnabled());
        assertEquals("", stepEditModal.getTargetPage());
        stepEditModal.setOrder(3);
        stepEditModal.save();
        stepEditModal = tourEditPage.editStep(2);
        assertEquals("Step 2", stepEditModal.getContent());
        stepEditModal.close();

        // Test that we can remove a step
        stepEditModal = tourEditPage.editStep(4);
        assertEquals("to remove", stepEditModal.getContent());
        stepEditModal.close();
        tourEditPage.deleteStep(4, true);
        stepEditModal = tourEditPage.editStep(4);
        assertEquals("Step 4", stepEditModal.getContent());
        stepEditModal.close();

        // Save the tour...
        tourEditPage.clickSaveAndView();

        // And let's try it!
        tourHomePage = TourHomePage.gotoPage();
        assertTrue(tourHomePage.getTours().contains(new TourFromLivetable("Test", "Tour.WebHome", true, "-")));

        PageWithTour homePage = new PageWithTour();
        assertTrue(homePage.isTourDisplayed());

        // Step 1
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Step 2
        homePage.nextStep();
        assertEquals("Title 2", homePage.getStepTitle());
        assertEquals("Step 2", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertTrue(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Go back to step 1
        homePage.previousStep();
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Go back to step 2
        homePage.nextStep();
        assertEquals("Title 2", homePage.getStepTitle());
        assertEquals("Step 2", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertTrue(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Step 3
        homePage.nextStep();
        assertEquals("Title 3", homePage.getStepTitle());
        assertEquals("Step 3", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertTrue(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Step 4
        homePage.nextStep();
        // Use a second page to test the Multipage feature
        PageWithTour secondPage = new PageWithTour();
        assertTrue(secondPage.getUrl().endsWith("TourCode/TourClass"));
        assertEquals("Title 4", secondPage.getStepTitle());
        assertEquals("Step 4", secondPage.getStepDescription());
        assertFalse(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertTrue(secondPage.hasEndButton());

        // End
        secondPage.end();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Resume (to step 4)
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Title 4", secondPage.getStepTitle());
        assertEquals("Step 4", secondPage.getStepDescription());
        assertFalse(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertTrue(secondPage.hasEndButton());

        // Close
        secondPage.close();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Go to an other page and then go back
        TourHomePage.gotoPage();
        secondPage = PageWithTour.gotoPage("TourCode", "TourClass");
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Resume (to step 4)
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Title 4", secondPage.getStepTitle());
        assertEquals("Step 4", secondPage.getStepDescription());
        assertFalse(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertTrue(secondPage.hasEndButton());

        // End
        secondPage.end();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Go back to the tour homepage
        TourHomePage.gotoPage();
        // Launch the tour
        homePage = tourHomePage.startTour("Test");
        // So the step 1 is active
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());
        homePage.close();

        cleanUp("Test");
    }

    @Test
    public void testBindedToClassTour() throws Exception
    {
        // First, we need to create a tour
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        TourEditPage tourEditPage = tourHomePage.addNewEntry("NewTest");
        setUpTour(tourEditPage, "Description", true, "", "TourCode.TourClass");
        setUpStep(tourEditPage, "body", "Tour Title", "Tour Content", true, "");
        tourEditPage.clickSaveAndView();

        tourHomePage = TourHomePage.gotoPage();
        assertTrue(tourHomePage.getTours().contains(new TourFromLivetable("NewTest", "-", true, "TourCode.TourClass")));

        PageWithTour homePage = PageWithTour.gotoPage("Tour", "NewTest");
        assertTrue(homePage.isTourDisplayed());
        homePage.end();

        cleanUp("NewTest");
    }
}
