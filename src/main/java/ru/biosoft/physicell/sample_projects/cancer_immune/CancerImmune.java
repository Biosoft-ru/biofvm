package ru.biosoft.physicell.sample_projects.cancer_immune;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.ui.Visualizer;

/**
###############################################################################
# If you use PhysiCell in your project, please cite PhysiCell and the version #
# number, such as below:                                                      #
#                                                                             #
# We implemented and solved the model using PhysiCell (Version x.y.z) [1].    #
#                                                                             #
# [1] A Ghaffarizadeh, R Heiland, SH Friedman, SM Mumenthaler, and P Macklin, #
#     PhysiCell: an Open Source Physics-Based Cell Simulator for Multicellu-  #
#     lar Systems, PLoS Comput. Biol. 14(2): e1005991, 2018                   #
#     DOI: 10.1371/journal.pcbi.1005991                                       #
#                                                                             #
# See VERSION.txt or call get_PhysiCell_version() to get the current version  #
#     x.y.z. Call display_citations() to get detailed information on all cite-#
#     able software used in your PhysiCell application.                       #
#                                                                             #
# Because PhysiCell extensively uses BioFVM, we suggest you also cite BioFVM  #
#     as below:                                                               #
#                                                                             #
# We implemented and solved the model using PhysiCell (Version x.y.z) [1],    #
# with BioFVM [2] to solve the transport equations.                           #
#                                                                             #
# [1] A Ghaffarizadeh, R Heiland, SH Friedman, SM Mumenthaler, and P Macklin, #
#     PhysiCell: an Open Source Physics-Based Cell Simulator for Multicellu-  #
#     lar Systems, PLoS Comput. Biol. 14(2): e1005991, 2018                   #
#     DOI: 10.1371/journal.pcbi.1005991                                       #
#                                                                             #
# [2] A Ghaffarizadeh, SH Friedman, and P Macklin, BioFVM: an efficient para- #
#     llelized diffusive transport solver for 3-D biological simulations,     #
#     Bioinformatics 32(8): 1256-8, 2016. DOI: 10.1093/bioinformatics/btv730  #
#                                                                             #
###############################################################################
#                                                                             #
# BSD 3-Clause License (see https://opensource.org/licenses/BSD-3-Clause)     #
#                                                                             #
# Copyright (c) 2015-2021, Paul Macklin and the PhysiCell Project             #
# All rights reserved.                                                        #
#                                                                             #
# Redistribution and use in source and binary forms, with or without          #
# modification, are permitted provided that the following conditions are met: #
#                                                                             #
# 1. Redistributions of source code must retain the above copyright notice,   #
# this list of conditions and the following disclaimer.                       #
#                                                                             #
# 2. Redistributions in binary form must reproduce the above copyright        #
# notice, this list of conditions and the following disclaimer in the         #
# documentation and/or other materials provided with the distribution.        #
#                                                                             #
# 3. Neither the name of the copyright holder nor the names of its            #
# contributors may be used to endorse or promote products derived from this   #
# software without specific prior written permission.                         #
#                                                                             #
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" #
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE   #
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE  #
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE   #
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR         #
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF        #
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS    #
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN     #
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)     #
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE  #
# POSSIBILITY OF SUCH DAMAGE.                                                 #
#                                                                             #
###############################################################################
*/

public class CancerImmune
{
    public static boolean use2D = true;

    public static void init(Model model) throws Exception
    {
        // use the same random seed so that future experiments have the 
        // same initial histogram of oncoprotein, even if threading means 
        // that future division and other events are still not identical 
        // for all runs 
        PhysiCellUtilities.setSeed( model.getParameterInt( "random_seed" ) );
        SignalBehavior.setup_signal_behavior_dictionaries( model.getMicroenvironment() );
        createCancerCell();
        createImmuneCell( model );
        setupTissue( model );
        model.addEvent( new ImmunityEvent( model.getParameterDouble( "immune_activation_time" ) ) );
        for( Visualizer visualizer : model.getVisualizers() )
        {
            visualizer.setAgentVisualizer( new CancerImmunityVisualizer() );
        }
    }

    static void createImmuneCell(Model model)
    {
        Microenvironment microenvironment = model.getMicroenvironment();
        CellDefinition cd = CellDefinition.getCellDefinition( "immune cell" );
        CellDefinition cancerCellCD = CellDefinition.getCellDefinition( "cancer cell" );

        int oxygen_ID = microenvironment.findDensityIndex( "oxygen" );
        int immuno_ID = microenvironment.findDensityIndex( "immunostimulatory factor" );

        // reduce o2 uptake 
        cd.phenotype.secretion.uptakeRates[oxygen_ID] *= model.getParameterDouble( "immune_o2_relative_uptake" );

        cd.phenotype.mechanics.cell_cell_adhesion_strength *= model.getParameterDouble( "immune_relative_adhesion" );
        cd.phenotype.mechanics.cell_cell_repulsion_strength *= model.getParameterDouble( "immune_relative_repulsion" );

        // figure out mechanics parameters 
        cd.phenotype.mechanics.relative_maximum_attachment_distance = cancerCellCD.custom_data.get( "max_attachment_distance" )
                / cd.phenotype.geometry.radius;

        cd.phenotype.mechanics.attachment_elastic_constant = cancerCellCD.custom_data.get( "elastic_coefficient" );

        cd.phenotype.mechanics.relative_detachment_distance = cancerCellCD.custom_data.get( "max_attachment_distance" )
                / cd.phenotype.geometry.radius;

        // set functions 
        cd.functions.updatePhenotype = null;
        cd.functions.custom_cell_rule = new ImmuneCellRule();
        cd.functions.update_migration_bias = new ImmuneCellMotility();
        cd.functions.contact_function = new AdhesionContact();
    }

    public static void createCancerCell() throws Exception
    {
        CellDefinition cd = CellDefinition.getCellDefinition( "cancer cell" );
        cd.parameters.o2_proliferation_saturation = 38.0;
        cd.parameters.o2_reference = 38.0;
        cd.phenotype.mechanics.relative_maximum_attachment_distance = cd.custom_data.get( "max_attachment_distance" )
                / cd.phenotype.geometry.radius;
        cd.phenotype.mechanics.relative_detachment_distance = cd.custom_data.get( "max_attachment_distance" )
                / cd.phenotype.geometry.radius;
        cd.phenotype.mechanics.attachment_elastic_constant = cd.custom_data.get( "elastic_coefficient" );
        cd.functions.updatePhenotype = new TumorPhenotype();
        cd.functions.custom_cell_rule = null;
        cd.functions.contact_function = new AdhesionContact();
        cd.functions.update_migration_bias = null;
    }

    //    static void create_cell_types(Model model) throws Exception
    //    {
    //        Microenvironment microenvironment = model.getMicroenvironment();
    //        createImmuneCellType( model );
    //        SignalBehavior.setup_signal_behavior_dictionaries( microenvironment );
    //        Rules.setup_cell_rules( model );
    //    }



    static List<double[]> createSpherePositions(double cellRadius, double sphereRadius)
    {
        List<double[]> cells = new ArrayList<>();
        int xc = 0, yc = 0, zc = 0;
        double xSpacing = cellRadius * Math.sqrt( 3 );
        double ySpacing = cellRadius * 2;
        double zSpacing = cellRadius * Math.sqrt( 3 );

        if( use2D )
        {
            for( double x = -sphereRadius; x < sphereRadius; x += xSpacing, xc++ )
            {
                for( double y = -sphereRadius; y < sphereRadius; y += ySpacing, yc++ )
                {
                    double[] tempPoint = new double[3];
                    tempPoint[0] = x + ( zc % 2 ) * 0.5 * cellRadius;
                    tempPoint[1] = y + ( xc % 2 ) * cellRadius;
                    tempPoint[2] = 0;

                    if( Math.sqrt( VectorUtil.norm_squared( tempPoint ) ) < sphereRadius )
                    {
                        cells.add( tempPoint );
                    }
                }
            }
        }
        else
        {
            for( double z = -sphereRadius; z < sphereRadius; z += zSpacing, zc++ )
            {
                for( double x = -sphereRadius; x < sphereRadius; x += xSpacing, xc++ )
                {
                    for( double y = -sphereRadius; y < sphereRadius; y += ySpacing, yc++ )
                    {
                        double[] tempPoint = new double[3];
                        tempPoint[0] = x + ( zc % 2 ) * 0.5 * cellRadius;
                        tempPoint[1] = y + ( xc % 2 ) * cellRadius;
                        tempPoint[2] = z;

                        if( Math.sqrt( VectorUtil.norm_squared( tempPoint ) ) < sphereRadius )
                        {
                            cells.add( tempPoint );
                        }
                    }
                }
            }
        }
        return cells;
    }

    static void setupTissue(Model model) throws Exception
    {
        // place a cluster of tumor cells at the center 
        CellDefinition cd = CellDefinition.getCellDefinition( "cancer cell" );
        double cellRadius = cd.phenotype.geometry.radius;
        //        double cell_spacing = 0.95 * 2.0 * cell_radius;

        double tumorRadius = model.getParameterDouble( "tumor_radius" );// 250.0;  

        List<double[]> positions = createSpherePositions( cellRadius, tumorRadius );
        System.out.println( "creating " + positions.size() + " closely-packed tumor cells ... " );

        double imm_mean = model.getParameterDouble( "tumor_mean_immunogenicity" );
        double imm_sd = model.getParameterDouble( "tumor_immunogenicity_standard_deviation" );

        Microenvironment m = model.getMicroenvironment();
        for( double[] position : positions )
        {
            Cell pCell = Cell.createCell( cd, m, position ); // tumor cell 
            double oncoprotein = Math.max( 0, PhysiCellUtilities.NormalRandom( imm_mean, imm_sd ) );
            pCell.custom_data.set( "oncoprotein", oncoprotein );
        }
        printSummary( m, "oncoprotein" );
    }

    private static void printSummary(Microenvironment m, String property)
    {
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        Set<Cell> cells = m.getAgents( Cell.class );
        for( Cell cell : cells )
        {
            double r = cell.custom_data.get( property );
            sum += r;
            min = Math.min( min, r );
            max = Math.max( max, r );
        }
        double mean = sum / ( cells.size() + 1e-15 );
        // compute standard deviation 
        sum = 0.0;
        for( Cell cell : cells )
        {
            sum += ( cell.custom_data.get( property ) - mean ) * ( cell.custom_data.get( property ) - mean );
        }
        double standardDeviation = Math.sqrt( sum / ( cells.size() - 1.0 + 1e-15 ) );

        System.out.println( "Oncoprotein summary: " );
        System.out.println( "===================" );
        System.out.println( "mean: " + mean );
        System.out.println( "standard deviation: " + standardDeviation );
        System.out.println( "[min max]: [" + min + " " + max + "]" );
    }
}