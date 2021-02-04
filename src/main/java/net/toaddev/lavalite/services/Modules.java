/*
 *  MIT License
 *
 *  Copyright (c) 2021 Toadless @ toaddev.net
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of Lavalite and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of Lavalite, and to permit persons to whom Lavalite is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Lavalite.
 *
 * LAVALITE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package net.toaddev.lavalite.services;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.toaddev.lavalite.entities.exception.ModuleNotFoundException;
import net.toaddev.lavalite.entities.module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *  This class loads all of the bots modules
 */
public class Modules
{
    private static final String MODULE_PACKAGE = "net.toaddev.lavalite.modules";
    private static final Logger LOG = LoggerFactory.getLogger(Modules.class);

    private final JDA main;
    public final List<Module> modules;

    public Modules(JDA main){
        this.main = main;
        this.modules = new LinkedList<>();
        loadModules();
    }

    private void loadModules()
    {
        LOG.info("Loading modules...");
        try(var result = new ClassGraph().acceptPackages(MODULE_PACKAGE).scan())
        {
            var queue = result.getSubclasses(Module.class.getName()).stream()
                    .map(ClassInfo::loadClass)
                    .filter(Module.class::isAssignableFrom)
                    .map(clazz ->
                    {
                        try
                        {
                            return ((Module) clazz.getDeclaredConstructor().newInstance()).init(this);
                        }
                        catch(Exception e)
                        {
                            LOG.info("Error whilst attempting to load modules.", e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedList::new));

            while(!queue.isEmpty())
            {
                var instance = queue.remove();
                var dependencies = instance.getDependencies();
                if(dependencies != null && !dependencies.stream().allMatch(mod -> this.modules.stream().anyMatch(module -> mod == module.getClass())))
                {
                    queue.add(instance);
                    LOG.info("Added '{}' back to the queue. Dependencies: {}", instance.getClass().getSimpleName(), dependencies.toString());
                    continue;
                }
                instance.onEnable();
                this.modules.add(instance);
            }
        }
        LOG.info("Finished loading {} modules", this.modules.size());
    }

    public Object[] getModules()
    {
        return this.modules.toArray();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz)
    {
        var module = this.modules.stream().filter(mod -> mod.getClass().equals(clazz)).findFirst();
        if(module.isEmpty()){
            throw new ModuleNotFoundException(clazz);
        }
        return (T) module.get();
    }

    public JDA getJDA()
    {
        return this.main;
    }

    public Guild getGuildById(long guildId)
    {
        return getJDA().getGuildById(guildId);
    }
}