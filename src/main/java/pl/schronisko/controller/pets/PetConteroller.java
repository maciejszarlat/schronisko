package pl.schronisko.controller.pets;


import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.schronisko.dao.AnimalDao;
import pl.schronisko.dao.SpeciesDao;
import pl.schronisko.entity.Animal;
import pl.schronisko.entity.Species;
import pl.schronisko.helpers.CustomerId;
import pl.schronisko.service.AnimalService;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class PetConteroller {

    @Autowired
    private CustomerId customerId;

    @Autowired
    private AnimalDao animalDao;

    @Autowired
    private AnimalService animalService;

    @Autowired
    private SpeciesDao speciesDao;

    @Autowired
    ServletContext servletContext;

    @GetMapping("/admin/pet-list")
    public String petList(Model theModel, HttpServletRequest request) {
        List<Animal> theAnimals = animalDao.getAnimal(request);

        theModel.addAttribute("animals", theAnimals);
        theModel.addAttribute("menuItem", "pet-list");

        return "admin/animals/pet-list";

    }

    @GetMapping("/admin/pet-add")
    public String petAdd(Model theModel) {

        Animal theAnimal = new Animal();

        List<Species> theSpecies = speciesDao.getSpecies();

        theModel.addAttribute("animal", theAnimal);
        theModel.addAttribute("specie", theSpecies);
        theModel.addAttribute("menuItem", "pet-add");

        return "admin/animals/pet-add";
    }

    @PostMapping("/admin/petSave")
    public String petSave(@Valid @ModelAttribute("animal") Animal theAnimal,
                          BindingResult theBindingResult,
                          HttpServletRequest request,
                          Model theModel
    ) {
        if (theBindingResult.hasErrors()) {
            List<Species> theSpecies = speciesDao.getSpecies();
            theModel.addAttribute("specie", theSpecies);
            return "admin/animals/pet-add";
        } else {
            theAnimal.setCustomerId(customerId.getCustomerId(request));

            MultipartFile uploadFile = theAnimal.getImage();

            String fileName = uploadFile.getOriginalFilename();
            String extension = FilenameUtils.getExtension(fileName);

            fileName = fileName.replace(fileName, System.currentTimeMillis() + "." + extension);
            long size = uploadFile.getSize();

            System.out.println("wilekość pliku: " + size + " a rozszerzenie to:    " + extension);

            if ((extension.equals("jpg") || extension.equals("jpeg")) && size < 2097152) {
                try {
                    final String destFolder = servletContext.getResource("/resources/dist/img/petsImages/").getPath();

                    String destFilePath = destFolder + fileName;
                    System.out.println("sciezka do pliku:   " + destFilePath);
                    File destFile = new File(destFilePath);

                    try {
                        uploadFile.transferTo(destFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    theAnimal.setPetImage(fileName);

                    animalService.saveTheAnimal(theAnimal);
                } catch (MalformedURLException e){
                    e.printStackTrace();
                }
                return "redirect:/admin/pet-list";
            } else {
                List<Species> theSpecies = speciesDao.getSpecies();
                theModel.addAttribute("specie", theSpecies);
                theModel.addAttribute("error", "Plik może mieć rozszerzenie jpg oraz nie może przekraczać 2MB");
                return "admin/animals/pet-add";
            }
        }
    }

    @GetMapping("/admin/petDelete/{id}")
    public String petDelete(@PathVariable int id, RedirectAttributes ra) {

        String fileName = animalService.getOneAnimal(id).getPetImage();
        animalService.deleteAnimal(id);

        String pathname = "/Users/maciejszarlat/Desktop/Projekty/schronikso-kopia/src/main/webapp/resources/dist/img/petsImages/" + fileName;;
        File file = new File(pathname);
        file.delete();

        ra.addFlashAttribute("success", "Zwierzak został usunięty");
        return "redirect:/admin/pet-list";
    }

    @GetMapping("/admin/petUpdateForm")
    public String petUpdateForm(@RequestParam("id") int id, Model model, RedirectAttributes ra) {

        boolean checkAnimal = animalService.checkAnimal(id);

        if (checkAnimal == true) {
            Animal theAnimal = animalService.getOneAnimal(id);

            List<Species> theSpecies = speciesDao.getSpecies();

            model.addAttribute("specie", theSpecies);

            model.addAttribute("animal", theAnimal);

            return "admin/animals/pet-add";
        } else {
            ra.addFlashAttribute("noPetFound", "Nie ma takiego zwierzaka");
            return "redirect:/admin/pet-list";
        }
    }

    @GetMapping("admin/singlePet")
    public String singlePet(@RequestParam("id") int id, Model model) {

        Animal theAnimal = animalService.getOneAnimal(id);

        model.addAttribute("theAnimal", theAnimal);

        return "admin/animals/single-pet";
    }

}
